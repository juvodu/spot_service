package com.juvodu.util;

/**
 * Generic constants are stored here
 *
 * @author Juvodu
 */
public class Constants {

    /** dynamo db indexes used for querying */
    public static final String SNS_APPLICATION_ARN = "arn:aws:sns:eu-central-1:980738030415:app/GCM/LetMeGo"; // SNS application arn for push notifications
    public static final String CONTINENT_COUNTRY_INDEX = "continent-country-index";
    public static final String CONTINENT_GEOHASH_INDEX = "continent-geohash-index";
    public static final String CONTINENT_CRONDATE_INDEX ="continent-crondate-index";
    public static final String USERNAME_TOPIC_INDEX ="username-topic-index";
    public static final String USERNAME_ENDPOINT_INDEX ="username-endpoint-index";

    /** number of max devices per user which receive push notifications */
    public static final int MAX_USER_DEVICES = 100;
    public static final String NOTIFICATION_TIME_TO_LIVE = "86400"; // 24 h
}
