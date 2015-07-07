//
//  HEQuery.h
//  Health Exporter
//
//  Created by Joseph Carson on 7/1/15.
//  Copyright (c) 2015 Joseph Carson. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "JSONModelLib.h"

/**
 * Base class to implement sorting, aggregation, and disambiguation (reification) of
 * HKObjectType objects that are returned from queries.
 */
@interface HKObjectAggregator : JSONModel;

//@property (nonatomic, readonly) NSArray * rawHKObjects;

// Derived implementations must be able to accept this method called multiple times for processing
// different groups of raw objects.  The derived implementation must observe the values and update
// its own internal values to reflect the given data how it deems fit.
- (void)processHKObjects:(NSArray *)hkObjects;

@end




typedef enum {
    NEW,
    QUERYING,
    DONE
} HEQueryState;


@protocol HEQueryDelegate


@end

typedef void (^hequery_completion_handler)(HKObjectAggregator *);

/**
 * Abstract class for executing a query and invoking callbacks based on their state.
 * This class should furnish common utility functions for derivations to make use of
 * to implement their query processing behavior.  The base class should also furnish
 * functionality to package and send the data.
 *
 * Derived classes should implement their results as properties on the object that are
 * populated as soon as query is complete.  As an example, the HESampleExporter should
 * make a list of quantities and characteristics as properties that are populated once
 * the query is complete and are accessed as such HESampleExporter.quantities.stepCount
 * is an NSArray or list of objects that HealthKit returns.
 */
@interface HEQuery : NSObject

@property (atomic, readonly) HEQueryState state;
@property (nonatomic, weak) id<HEQueryDelegate> delegate;

// Execute the given query.
- (void) executeQuery:(hequery_completion_handler)completionBlock;

- (void) updateState:(HEQueryState)newState;

@end
