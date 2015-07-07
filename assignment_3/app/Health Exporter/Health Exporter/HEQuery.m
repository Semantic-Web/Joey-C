//
//  HEQuery.m
//  Health Exporter
//
//  Created by Joseph Carson on 7/1/15.
//  Copyright (c) 2015 Joseph Carson. All rights reserved.
//

#import "HEQuery.h"

// Fantastic convetions in emulating abstract class concepts in Objective-C!
// http://stackoverflow.com/questions/1034373/creating-an-abstract-class-in-objective-c
#define mustOverride() @throw [NSException exceptionWithName:NSInvalidArgumentException reason:[NSString stringWithFormat:@"%s must be overridden in a subclass/category", __PRETTY_FUNCTION__] userInfo:nil]
#define methodNotImplemented() mustOverride()

@implementation HKObjectAggregator

//@synthesize rawHKObjects = _rawHKObjects;

- (void)processHKObjects:(NSArray *)hkObjects
{
    methodNotImplemented();
}

@end


@implementation HEQuery

@synthesize state = _state;

-(instancetype) init
{
    self = [super init];
    if ( self ) {
        [self updateState:NEW];
    }
    
    return self;
}

- (void) updateState:(HEQueryState)newState
{
    // TODO: Manually call the functions to publish state change.
    // http://stackoverflow.com/questions/549672/is-it-possible-to-observe-a-readonly-property-of-an-object-in-cocoa-touch
    _state = newState;
}

- (void)executeQuery:(hequery_completion_handler)completionBlock {
    NSLog(@"HEExporter executeQuery base implementation invoked.  Override and run queries against HEHealthKitController!");
    methodNotImplemented();
}

@end
