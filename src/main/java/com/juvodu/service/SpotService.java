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
import com.juvodu.database.model.*;
import com.juvodu.util.Constants;
import com.juvodu.util.GeoHelper;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Spot retrieval and processing.
 *
 * @author Juvodu
 */
public class SpotService<T extends BaseSpot> {

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

        String filterExpression = "continent = :val1";
        DynamoDBQueryExpression<T> queryExpression = databaseHelper.createQueryExpression(continent.getCode(),
                null, Constants.CONTINENT_COUNTRY_INDEX, filterExpression, 100);
        return mapper.queryPage(spotClass, queryExpression).getResults();
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

        String filterExpression = "continent = :val1 and country = :val2";
        DynamoDBQueryExpression<T> queryExpression = databaseHelper.createQueryExpression(continent.getCode(),
                country.getCode(), Constants.CONTINENT_COUNTRY_INDEX, filterExpression, 100);

        return mapper.queryPage(spotClass, queryExpression).getResults();
    }

    /**
     * Find all spots in a given radius
     *
     * @param continent
     *          the continent in which the search takes place (partition key of continent-geohash-index table)
     * @param position
     *          which is the center of the radius
     * @param searchRadius
     *          search radius in km
     *
     * @return list of spots within the specifed radius
     */
    public List<T> findByDistance(Continent continent, Position position, int searchRadius){

        int searchRadiusMeter = searchRadius * 1000;
        List<T> spots = new LinkedList<>();
        GeoHashCircleQuery geoHashCircleQuery = new GeoHashCircleQuery(new WGS84Point(position.getLatitude(), position.getLongitude()), searchRadiusMeter);
        List<GeoHash> searchHashes = geoHashCircleQuery.getSearchHashes();

        for(GeoHash geoHash : searchHashes){

            //rough and fast filtering by geohash
            String binaryHashString = geoHash.toBinaryString();
            String filterExpression = "continent = :val1 and begins_with(geohash,:val2)";
            DynamoDBQueryExpression<T> queryExpression = databaseHelper.createQueryExpression(continent.getCode(),
                    binaryHashString, Constants.CONTINENT_GEOHASH_INDEX, filterExpression, 10);
            spots.addAll(mapper.queryPage(spotClass, queryExpression).getResults());
        }

        // calculate distance to each spot in km
        spots.forEach(spot -> spot.setDistance(GeoHelper.getDistance(position, spot.getPosition())/1000));

        // fine filtering and sorting by distance
        spots = spots.stream()
                .filter(spot -> searchRadius >= spot.getDistance())
                .sorted(Comparator.comparing(T::getDistance))
                .collect(Collectors.toList());

        return spots;
    }
}
