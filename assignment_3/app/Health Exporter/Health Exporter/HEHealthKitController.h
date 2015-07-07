//
//  HEHealthKitController.h
//  Health Exporter
//
//  Created by Joseph Carson on 6/23/15.
//  Copyright (c) 2015 Joseph Carson. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "HEQuery.h"

@class HKQuery;
@class HKSampleQuery;

typedef void (^hk_completion_handler)(BOOL, NSError *);
typedef void (^hk_sample_query_result_handler)(HKSampleQuery *query, NSArray *results, NSError *error);

/**
 * Singleton wrapper class for accessing HKHealthStore.  Ideally, this class would be able to be set
 * the desired health characteristics for tracking.  This class keeps lists of all types that it will
 * attempt to track according to their identifiers.
 */
@interface HEHealthKitController : NSObject

+ (instancetype) instance;
+ (BOOL) isHealthDataAvailable;

@property (nonatomic, readonly, copy) NSDictionary * characteristicTypes;
@property (nonatomic, readonly, copy) NSDictionary * quantityTypes;

- (void)executeQuery:(HKQuery *)hkQuery;

@end
