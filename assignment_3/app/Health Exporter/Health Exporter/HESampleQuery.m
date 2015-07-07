//
//  HESampleQuery.m
//  Health Exporter
//
//  Created by Joseph Carson on 7/2/15.
//  Copyright (c) 2015 Joseph Carson. All rights reserved.
//

#import <HealthKit/HealthKit.h>
#import "HEHealthKitController.h"
#import "HESampleQuery.h"
#import "JSONModelLib.h"

@implementation JSONStepCount

@end

@implementation HESampleAggregator

@synthesize stepCounts = _stepCounts;

- (instancetype) init
{
    self = [super init];
    if ( self ) {
        _stepCounts = (NSArray<JSONStepCount> *)[[NSMutableArray alloc] init];
    }
    return self;
}

- (void) processHKObjects:(NSArray *)hkObjects
{
    //NSLog(@"HESampleAggregator: processHKObjects: count: %lu", hkObjects.count);
    
    for ( HKSample * sample in hkObjects ) {
        NSString * sampleType = sample.sampleType.identifier;
        if ( [sampleType isEqualToString:HKQuantityTypeIdentifierStepCount] ) {
            [self processStepCount:(HKQuantitySample *)sample];
        }
        else if ( [sampleType isEqualToString:HKQuantityTypeIdentifierDietaryEnergyConsumed] ) {
            [self processDietaryEnergyIntake:(HKQuantitySample *)sample];
        }
        else {
            NSLog(@"Unsupported sample type: %@", sampleType);
        }
    }
}

- (void) processStepCount:(HKQuantitySample *)quantity
{
    JSONStepCount * stepCount = [[JSONStepCount alloc] init]; 
    stepCount.count = [quantity.quantity doubleValueForUnit:[HKUnit countUnit]];
    stepCount.startDate = quantity.startDate;
    stepCount.endDate = quantity.endDate;
    [((NSMutableArray *)_stepCounts) addObject:stepCount];
    
    //NSLog(@"%@", [[_stepCounts lastObject] toJSONString]);
}

- (void) processDietaryEnergyIntake:(HKQuantitySample *)quantity
{
    
}

@end



@implementation HESampleQuery
{
    @private
    NSDictionary * _characteristics;
    NSDictionary * _quantities;
}


- (instancetype) initWithCharacteristics:(NSDictionary *)characteristics andQuantities:(NSDictionary *)quantities
{
    self = [super init];
    if ( self ) {
        _characteristics = characteristics;
        _quantities = quantities;
    }
    
    return self;
}

- (void) executeQuery:(hequery_completion_handler)completionBlock {
    
    if ( self.state == NEW ) {
        // Update the state first.
        [self updateState:QUERYING];

        __block NSMutableArray * tickets = [[NSMutableArray alloc] init];
        __block HESampleAggregator * aggregator = [[HESampleAggregator alloc] init];
        NSMutableArray * queries = [[NSMutableArray alloc] init];
        
        // Loop through each of the quantities and build an HKSampleQuery
        for ( HKSampleType * q in _quantities.allValues ) {
            
            if ( [q isKindOfClass:[HKSampleType class]] ) {
                
                [tickets addObject:@""];
                
                // 60 seconds per min, 60 mins per hour, 24 hours per day, 5 days.
                static NSTimeInterval offset = 60 * 60 * 24 * 5;
                
                // This could be more accurate by taking the NSDateComponents just for day, month, and
                // year such that it would produce the NSDate for midnight 5 days ago.  Whatev...
                NSDate * severalDaysAgo = [NSDate dateWithTimeIntervalSinceNow:-offset];
                NSPredicate * predicate = [HKQuery predicateForSamplesWithStartDate:severalDaysAgo
                                                                            endDate:[NSDate distantFuture]
                                                                            options:HKQueryOptionNone];
                
                hk_sample_query_result_handler completionHandler = ^(HKSampleQuery *query, NSArray *results, NSError *error) {
                    
                    
                    NSLog(@"processing.  error: %@", error);
                    
                    [aggregator processHKObjects:results];
                    
                    NSLog(@"done processing.");
                    
                    // Remove ourselves.
                    [tickets removeLastObject];
                    if ( tickets.count == 0 ) {
                        // Oh, I'm the last one.  I guess we're done...
                        NSLog(@"all blocks complete.  invoke completion block.");
                        
                        // We have this aggregator all populated with JSON serializable objects.
                        // This is effectively the data we want to give to Raspberry Pi.
                        [self updateState:NEW];
                        completionBlock(aggregator);
                    }
                };
                
                NSSortDescriptor * sortDescriptor = [NSSortDescriptor sortDescriptorWithKey:HKSampleSortIdentifierStartDate ascending:NO];
                HKSampleQuery * query = [[HKSampleQuery alloc] initWithSampleType:q
                                                                        predicate:predicate
                                                                            limit:0
                                                                  sortDescriptors:@[sortDescriptor]
                                                                   resultsHandler:completionHandler];
                [queries addObject:query];
            }
        }
        
        for (HKSampleQuery * query in queries ) {
            [[HEHealthKitController instance] executeQuery:query];
        }
    } else {
        NSLog(@"Can't start a new query when one is already running.");
    }
}

@end
