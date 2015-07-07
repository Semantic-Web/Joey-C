//
//  HEHealthKitController.m
//  Health Exporter
//
//  Created by Joseph Carson on 6/23/15.
//  Copyright (c) 2015 Joseph Carson. All rights reserved.
//

#import "HEHealthKitController.h"
#import <HealthKit/HealthKit.h>

static HEHealthKitController * s_instance;

static NSString * CHARACTERISTIC_ID_LIST_KEY = @"hk_charactaristics";
static NSString * QUANTITY_ID_LIST_KEY = @"hk_charactaristics";

@interface HEHealthKitController ()

- (instancetype) initPrivate;
- (void) setCharacteristics:(NSSet *)charIDs quantityTypes:(NSSet *)quantityTypeIDs;
- (void) authorizeAndSaveCharacteristics:(NSDictionary *) hkCharacteristicMap andQuantities:(NSDictionary *)hkQuantityMap;

@end

@implementation HEHealthKitController
{
    @private
    HKHealthStore * m_healthStore;
    NSUserDefaults * m_defaults;
}

@synthesize characteristicTypes = _characteristicTypes;
@synthesize quantityTypes = _quantityTypes;

+(BOOL) isHealthDataAvailable
{
    return [HKHealthStore isHealthDataAvailable];
}

+(instancetype)instance
{
    if ( !s_instance )
    {
        s_instance = [[HEHealthKitController alloc] initPrivate];
        if ( s_instance )
        {
            
        }
    }
    
    return s_instance;
}

-(instancetype) initPrivate
{
    self = [super init];
    if ( self ) {
        NSLog(@"Creating HKHealthStore");
        m_healthStore = [[HKHealthStore alloc] init];
        [self setCharacteristics:[self getDefaultCharacteristics] quantityTypes:[self getDefaultQuantities]];
    }
    return self;
}

-(NSSet *) getDefaultCharacteristics
{
    // TODO:  Either read an NSSet of HKObjectTypes from NSUserDefaults or read use these default types.
    NSSet * hkObjsToRead = [[NSSet alloc] initWithObjects: HKCharacteristicTypeIdentifierDateOfBirth, HKCharacteristicTypeIdentifierBloodType, HKCharacteristicTypeIdentifierBiologicalSex, nil];
    return hkObjsToRead;
}

-(NSSet *) getDefaultQuantities
{
    NSSet * hkObjsToRead = [[NSSet alloc] initWithObjects:HKQuantityTypeIdentifierStepCount, HKQuantityTypeIdentifierDietaryEnergyConsumed, nil];
    return hkObjsToRead;
}

// Sets up tracking for the characteristics and quantity types that we will be tracking.
- (void) setCharacteristics:(NSSet *)charIDs quantityTypes:(NSSet *)quantityTypeIDs
{
    NSMutableDictionary * hkCharactaristics = [[NSMutableDictionary alloc] init];
    // What characteristics are we tracking?
    for ( NSString * charID in charIDs )
    {
        HKObjectType * type = [HKObjectType characteristicTypeForIdentifier:charID];
        if ( type ) {
            hkCharactaristics[charID] = type;
        }
    }
    
    NSMutableDictionary * hkQuantities = [[NSMutableDictionary alloc] init];
    // What quantity types are we tracking?
    for ( NSString * quanType in quantityTypeIDs )
    {
        HKObjectType * type = [HKObjectType quantityTypeForIdentifier:quanType];
        if ( type ) {
            hkQuantities[quanType] = type;
        }
    }
 
    [self authorizeAndSaveCharacteristics:hkCharactaristics andQuantities:hkQuantities];
}

-(void)authorizeAndSaveCharacteristics:(NSDictionary *) hkCharacteristicMap
                         andQuantities:(NSDictionary *) hkQuantityMap
{
    
    NSMutableSet * hkObjsToRead = [[NSMutableSet alloc] init];
    __block NSDictionary * charMap = hkCharacteristicMap;
    __block NSDictionary * quantMap = hkQuantityMap;
    
    for ( HKObjectType * obj in [hkCharacteristicMap allValues] ) {
        [hkObjsToRead addObject:obj];
    }
    
    for ( HKObjectType * obj in [hkQuantityMap allValues] ) {
        [hkObjsToRead addObject:obj];
    }
    
    hk_completion_handler completion = ^(BOOL success, NSError * err) {
        
        // TODO:  Add a property API to read the HKObjectTypes being tracked.
        NSUserDefaults * defaults = [NSUserDefaults standardUserDefaults];
        
        if ( !err ) {
            // We have no way of telling whether or not the user denied access for reading health data.
            // This is to prevent leakage of sensitive health data.  Therefore we should simply update our list
            // of HKObjectTypes that we would like to be tracking.
            [defaults setObject:[charMap allKeys]  forKey:CHARACTERISTIC_ID_LIST_KEY];
            [defaults setObject:[quantMap allKeys] forKey:QUANTITY_ID_LIST_KEY];
            
            _characteristicTypes = [charMap copy];
            _quantityTypes = [quantMap copy];
            
        } else {
            NSLog(@"completion: %d error: %@", success, err);
            [defaults setObject:[[NSArray alloc] init]  forKey:CHARACTERISTIC_ID_LIST_KEY];
            [defaults setObject:[[NSArray alloc] init] forKey:QUANTITY_ID_LIST_KEY];
            
            _characteristicTypes = [[NSDictionary alloc] init];
            _quantityTypes = [[NSDictionary alloc] init];
            
        }
        
        [defaults synchronize];
        //[self testQuery];
    };
    
    [m_healthStore requestAuthorizationToShareTypes:nil readTypes:hkObjsToRead completion:completion];
}

- (void)executeQuery:(HKQuery *)hkQuery
{
    [m_healthStore executeQuery:hkQuery];
}

-(void) testQuery
{
    NSCalendar *calendar = [NSCalendar currentCalendar];
    
    NSDate *now = [NSDate date];
    
    NSDateComponents *components = [calendar components:NSCalendarUnitYear|NSCalendarUnitMonth|NSCalendarUnitDay fromDate:now];
    
    NSDate *refDate = [calendar dateFromComponents:components];
    
    NSDate * startDate = [calendar dateByAddingUnit:NSCalendarUnitDay value:-2 toDate:refDate options:0];
    
    NSDate *endDate = [calendar dateByAddingUnit:NSCalendarUnitDay value:2 toDate:startDate options:0];
    
    HKSampleType *sampleType = [HKSampleType quantityTypeForIdentifier:HKQuantityTypeIdentifierStepCount];
    NSPredicate *predicate = [HKQuery predicateForSamplesWithStartDate:startDate endDate:endDate options:HKQueryOptionNone];
    
    HKSampleQuery *query = [[HKSampleQuery alloc] initWithSampleType:sampleType predicate:predicate limit:0 sortDescriptors:nil resultsHandler:^(HKSampleQuery *query, NSArray *results, NSError *error) {
        if (!results) {
            NSLog(@"An error occured fetching the user's tracked food. In your app, try to handle this gracefully. The error was: %@.", error);
            abort();
        }
        
        dispatch_async(dispatch_get_main_queue(), ^{
            
            for (HKQuantitySample *sample in results) {
                
                NSLog(@"result: stype: %@ qtype: %@ quantity: %@", [sample sampleType], [sample quantityType], [sample quantity]    );
            }
        });
    }];
    
    [m_healthStore executeQuery:query];
}

@end
