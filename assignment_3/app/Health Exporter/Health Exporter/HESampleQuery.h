//
//  HESampleQuery.h
//  Health Exporter
//
//  Created by Joseph Carson on 7/2/15.
//  Copyright (c) 2015 Joseph Carson. All rights reserved.
//

#import "HEQuery.h"
#import "JSONModelLib.h"

@protocol JSONStepCount

@end

@interface JSONStepCount : JSONModel

@property (nonatomic) double count;
@property (nonatomic, copy) NSDate * startDate;
@property (nonatomic, copy) NSDate * endDate;

@end

// HKObjectAggregator for processing HKSampleType objects.
@interface HESampleAggregator : HKObjectAggregator

@property (nonatomic) NSArray<JSONStepCount> * stepCounts;

@end


@interface HESampleQuery : HEQuery

//@property (nonatomic, readonly) biologicalSex;

-(instancetype) initWithCharacteristics:(NSDictionary *)characteristics andQuantities:(NSDictionary *)quantities;

@end
