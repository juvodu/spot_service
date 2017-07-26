package com.juvodu.service;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.queries.GeoHashCircleQuery;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.juvodu.database.DatabaseHelper;
import com.juvodu.database.model.Continent;
import com.juvodu.database.model.Country;
import com.juvodu.database.model.Spot;
import com.juvodu.util.Constants;

import java.util.LinkedList;
import java.util.List;

/**
 * Service for Spot retrieval and processing.
 *
 * @author Juvodu
 */
public class SpotService<T extends Spot> {

    private final DynamoDBMapper mapper;
    private final Class<T> spotClass;
    private final DatabaseHelper<T> databaseHelper;

    public SpotService(Class<T> spotClass){

        this.spotClass = spotClass;
        this.databaseHelper = new DatabaseHelper<>();
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.EU_CENTRAL_1)
                .build();
        this.mapper = new DynamoDBMapper(client);
    }

    /**
     * Retrieve a spot by its hash key
     *
     * @param id
     *          of the spot
     *
     * @return the spot model populated with data
     */
    public T getSpotById(String id){

        return mapper.load(spotClass, id);
    }

    /**
     * Save or update a spot instance
     *
     * @param spot
     *          the spot to save, if an item with the same id exists it will be updated
     *
     * @return the generated id (UUID) of the spot item
     */
    public String save(Spot spot){

        //create a geohash for each spot for fast queries based on position
        String base32GeoHash = databaseHelper.createBinaryGeohash(spot.getPosition());
        spot.setGeohash(base32GeoHash);

        // save does not return, instead it populates the generated id to the passed spot instance
        mapper.save(spot);

        return spot.getId();
    }

    /**
     * Delete the spot instance
     *
     * @param spot
     *          the spot instance to delete
     */
    public void delete(T spot){

        mapper.delete(spot);
    }

    /**
     * Delete all table entries - for testing purposes only
     */
    public void deleteAll(){

        for(T spot : findAll()){
            delete(spot);
        }
    }

    /**
     * Return all available spots, scan requests are potentially slow
     *
     * @return list of spots saved in the DB
     */
    public List<T> findAll(){

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return mapper.scan(spotClass, scanExpression);
    }

    /**
     * Find all spots for a given continent
     *
     * @param continent
     *          used to filter spots
     *
     * @return list of spots in the continent
     */
    public List<T> findByContinent(Continent continent){

        DynamoDBQueryExpression<T> queryExpression = databaseHelper.createQueryExpression(continent.getCode(),
                null, Constants.CONTINENT_COUNTRY_INDEX, "continent = :val1");
        return mapper.query(spotClass, queryExpression);
    }

    /**
     * Find all spots for a given country
     *
     * @param continent
     *              needs to be specified as it is the partition key of the continent-index
     * @param country
     *              the country to filter for, can be used as it is the range key of the continent-index
     *
     * @return list of spots filtered by the specified country
     */
    public List<T> findByCountry(Continent continent, Country country){

        DynamoDBQueryExpression<T> queryExpression = databaseHelper.createQueryExpression(continent.getCode(),
                country.getCode(), Constants.CONTINENT_COUNTRY_INDEX, "continent = :val1 and country = :val2");

        return mapper.query(spotClass, queryExpression);
    }

    /**
     * Find all spots in a given radius
     *
     * @param continent
     *          the continent in which the search takes place (partition key of continent-geohash-index table)
     * @param position
     *          which is the center of the radius
     * @param radius
     *          search radius in meter
     *
     * @return list of spots within the specifed radius
     */
    public List<T> findInRadius(Continent continent, WGS84Point position, int radius){

        List<T> spots = new LinkedList<>();

        GeoHashCircleQuery geoHashCircleQuery = new GeoHashCircleQuery(position, radius);
        List<GeoHash> searchHashes = geoHashCircleQuery.getSearchHashes();

        for(GeoHash geoHash : searchHashes){

            String binaryHashString = geoHash.toBinaryString();
            DynamoDBQueryExpression<T> queryExpression = databaseHelper.createQueryExpression(continent.getCode(),
                    binaryHashString, Constants.CONTINENT_GEOHASH_INDEX, "continent = :val1 and begins_with(geohash,:val2)");
            spots.addAll(mapper.query(spotClass, queryExpression));
        }

        //TODO: querying by geohash will return more spots than in the actual
        // radius as spots in the boundingbox of the geohash are also included
        //=> apply additional distance filtering

        return spots;
    }
}