//
//  SampleQueryViewController.m
//  Health Exporter
//
//  Created by Joseph Carson on 6/28/15.
//  Copyright (c) 2015 Joseph Carson. All rights reserved.
//

#import <HealthKit/HealthKit.h>
#import "SampleQueryViewController.h"
#import "HEHealthKitController.h"
#import "HESampleQuery.h"
#import <MessageUI/MessageUI.h>

#define CHARACTERISTICS_KEY @"Characteristics"
#define QUANTITIES_KEY @"Quantities"
#define SAMPLE_SWITCH_REUSEID @"sample_switch_cell"

@interface SampleQueryViewController ()
{
    HEHealthKitController * _hkkc;
    HESampleQuery * _sampleQuery;
    UIProgressView * _progressView;
    NSArray * _tableIndex;
    NSMutableDictionary * _switchHKObjMap;
}

-(void)buildTableIndex;

@end



@implementation SampleQueryViewController

@synthesize tableView = _tableView;
@synthesize shareAction = _shareAction;

- (instancetype)initWithCoder:(NSCoder *)aDecoder
{
    self = [super initWithCoder:aDecoder];
    if ( self ) {
        _hkkc = [HEHealthKitController instance];
        _switchHKObjMap = [[NSMutableDictionary alloc] init];
    }
    
    return self;
}

-(void)loadView
{
    [super loadView];
    
    // Emulate UITableViewController...
    CGRect screenBounds = [UIScreen mainScreen].bounds;
    CGRect toolbarFrame = CGRectMake(0, 0, screenBounds.size.width, 80);
    CGRect tableFrame = CGRectMake(0, 60, screenBounds.size.width, screenBounds.size.height - toolbarFrame.size.height);
    
    _tableView = [[UITableView alloc] initWithFrame:tableFrame style:UITableViewStyleGrouped];
    _tableView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    _tableView.backgroundColor = [UIColor lightGrayColor];
    _tableView.delegate = self;
    _tableView.dataSource = self;
    
    UIToolbar * tb = [[UIToolbar alloc] initWithFrame:toolbarFrame];
    tb.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    
    UIBarButtonItem * spacer = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    _progressView = [[UIProgressView alloc] initWithProgressViewStyle:UIProgressViewStyleBar];
    _progressView.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    _progressView.progress = 0.6;

    UIBarButtonItem * progressItem = [[UIBarButtonItem alloc] initWithCustomView:_progressView];
    
    _shareAction = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAction target:self action:@selector(runQuery)];
    [tb setItems:@[progressItem, spacer, _shareAction]];
    
    self.view.backgroundColor = [UIColor lightGrayColor];
    [self.view addSubview:tb];
    [self.view addSubview:_tableView];
    
    self.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Sample Query" image:nil tag:0];
    [self buildTableIndex]; // TODO: Need to listen to changes on the health store.
}

- (void)runQuery
{
    NSLog(@"run query");
    NSMutableDictionary * characteristics = [[NSMutableDictionary alloc] init];
    NSMutableDictionary * quantities = [[NSMutableDictionary alloc] init];
    
    for ( HKObjectType * hkObj in [_switchHKObjMap allKeys] ) {
        if ( [_switchHKObjMap[hkObj] isKindOfClass:[UISwitch class]] ) {
            UISwitch * switchObj = _switchHKObjMap[hkObj];
            if ( switchObj && switchObj.isOn ) {
                if ( [hkObj isKindOfClass:[HKCharacteristicType class]] ) {
                    
                    characteristics[hkObj.identifier] = [hkObj copy];
                } else if ( [hkObj isKindOfClass:[HKQuantityType class]] ) {
                    
                    quantities[hkObj.identifier] = [hkObj copy];
                } else {
                    
                    NSLog(@"Unrecognized type of HKObject is selected!  Ingnoring it!");
                }
            }
        }
    }
    
    hequery_completion_handler completionBlock = ^(HKObjectAggregator * resultAggregator) {
        // I'm a ViewController, ergo I should be running on the main thread.
        __block HESampleAggregator * sampleAggregator = (HESampleAggregator *) resultAggregator;
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^(){
            
            // Find the paths.  Take the first one and append a directory separator.
            NSArray * paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString * docsDirPath = [paths[0] stringByAppendingString:@"/"];

            // Build a unique file name.
            NSDate * now = [NSDate date];
            NSString * fileName = [NSString stringWithFormat:@"health_data_export_%@.json", now];
            NSString * fullPath = [docsDirPath stringByAppendingString:fileName];

            NSError * err = nil;  // JSON serialize the aggregator and write it to a file.
            NSString * json = [sampleAggregator toJSONString];
            [json writeToFile:fullPath atomically:YES encoding:NSASCIIStringEncoding error:&err];
            
            if ( err ) {
                NSLog(@"Failed writing to %@", fullPath);
            } else {
                
                NSLog(@"Successfully wrote to %@", fullPath);
                
                // Invoke the email application to compose an email with the JSON file attached.
                if ( [MFMailComposeViewController canSendMail] ) {
                    MFMailComposeViewController * mailController = [[MFMailComposeViewController alloc] init];
                    NSData * attachmentData = [[NSData alloc] initWithContentsOfFile:fullPath];

                    [mailController setMailComposeDelegate:self];
                    [mailController setSubject:fileName];
                    
                    [mailController addAttachmentData:attachmentData mimeType:@"application/json" fileName:fileName];
                    [self presentViewController:mailController animated:YES completion:^(void) {
                        NSLog(@"compose mail form is up.");
                    }];
                }
            }
        });
    };
    
    if ( !_sampleQuery ) {
        _sampleQuery = [[HESampleQuery alloc] initWithCharacteristics:characteristics andQuantities:quantities];
    }
    
    [_sampleQuery executeQuery: [completionBlock copy]];
}

- (void)mailComposeController:(MFMailComposeViewController *)controller
          didFinishWithResult:(MFMailComposeResult)result
                        error:(NSError *)error
{
    [self dismissViewControllerAnimated:YES completion:nil];
}


/**
 * Inspects what characteristics are available and builds the index
 * where each group of properties are associated with a section index.
 */
-(void) buildTableIndex
{
    NSUInteger sectionIndex = 0;
    NSMutableArray * index = [[NSMutableArray alloc] init];

    if ( _hkkc.characteristicTypes.count > 0 ) {
        index[sectionIndex++] = CHARACTERISTICS_KEY;
    }
    
    if ( _hkkc.quantityTypes.count > 0 ) {
        index[sectionIndex++] = QUANTITIES_KEY;
    }
    
    _tableIndex = index;
    [_switchHKObjMap removeAllObjects];
}


- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return _tableIndex.count;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if ( section < _tableIndex.count ) {
        
        NSString * type = _tableIndex[section];
        
        if ( [type isEqualToString:CHARACTERISTICS_KEY] ) {
            
            return _hkkc.characteristicTypes.count;
            
        } else if ( [type isEqualToString:QUANTITIES_KEY] ) {
            
            return _hkkc.quantityTypes.count;
        }
        
    } else {
        
        NSLog(@"invalid section index: %ld", (long)section);
    }
    
    return 0;
}

- (CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section
{
    return 20.0;
}

- (UIView *)tableView:(UITableView *)tableView viewForHeaderInSection:(NSInteger)section
{
    UILabel * label = nil;
    if ( section < [_tableIndex count] ) {
        label = [[UILabel alloc] initWithFrame:CGRectMake(0, 0, 300, 20)];
        label.text = _tableIndex[section];
        label.textColor = [UIColor whiteColor];
    }
    
    return label;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    
    // 1.  Create or retrieve a cached cell.
    UITableViewCell * cell = [self createCell];
    
    // 2.  Update the cell based on what it should be for index path.
    [self populateCell:cell forIndexPath:indexPath];
    
    return cell;
}

- (void)populateCell:(UITableViewCell *)cell forIndexPath:(NSIndexPath *)indexPath
{
    NSUInteger section = [indexPath indexAtPosition:0];
    NSUInteger groupItemIndex = [indexPath indexAtPosition:1];
    
    if ( section < [_tableIndex count]) {
        NSString * key = _tableIndex[section];
        if ( [key isEqualToString:CHARACTERISTICS_KEY] ) {
            if ( groupItemIndex < [_hkkc.characteristicTypes count] ) {
                NSString * idLabel = _hkkc.characteristicTypes.allKeys[groupItemIndex];
                NSString * label = [idLabel copy];
                NSRange prefixRange = [idLabel rangeOfString:@"HKCharacteristicTypeIdentifier"];
                if ( prefixRange.location != NSNotFound ) {
                    label = [idLabel substringFromIndex:prefixRange.length];
                }

                cell.textLabel.text = label;
                _switchHKObjMap[ _hkkc.characteristicTypes[idLabel] ] = cell.accessoryView;
            }
        } else if ( [key isEqualToString:QUANTITIES_KEY] ) {
            if ( groupItemIndex < [_hkkc.quantityTypes count] ) {
                NSString * idLabel = _hkkc.quantityTypes.allKeys[groupItemIndex];
                NSString * label = [idLabel copy];
                NSRange prefixRange = [label rangeOfString:@"HKQuantityTypeIdentifier"];
                if ( prefixRange.location != NSNotFound ) {
                    label = [label substringFromIndex:prefixRange.length];
                }
                
                cell.textLabel.text = label;
                _switchHKObjMap[ _hkkc.quantityTypes[idLabel] ] = cell.accessoryView;
            }
        }
    }
}

- (UITableViewCell *)createCell
{
    UITableViewCell * cell = [_tableView dequeueReusableCellWithIdentifier:SAMPLE_SWITCH_REUSEID];
    if ( !cell ) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:SAMPLE_SWITCH_REUSEID];
        cell.selectionStyle = UITableViewCellSelectionStyleNone;
        cell.backgroundColor = [UIColor colorWithWhite:1.0 alpha:0.5];
        cell.textLabel.textColor = [UIColor whiteColor];
        
        // Build the switch.
        UISwitch * toggle = [[UISwitch alloc] initWithFrame:CGRectMake(0, 0, 40, 40)];
        [toggle addTarget:self action:@selector(toggleChanged:) forControlEvents:UIControlEventValueChanged];
        cell.accessoryView = toggle;
    }
    
    cell.textLabel.text = @"Default";
    return cell;
}

- (void)toggleChanged:(UISwitch *)switchObj
{
    NSLog(@"toggle changed: %d",switchObj.on);
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
