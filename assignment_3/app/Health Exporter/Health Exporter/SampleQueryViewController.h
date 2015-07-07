//
//  SampleQueryViewController.h
//  Health Exporter
//
//  Created by Joseph Carson on 6/28/15.
//  Copyright (c) 2015 Joseph Carson. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <MessageUI/MessageUI.h>
//@import MessageUI;

/**
 * Hacky version of UITableViewController since the stupid xib editor doesn't support
 * building the UITableView with grouped style.  Go apple :P
 */
@interface SampleQueryViewController : UIViewController<UITableViewDataSource, UITableViewDataSource, MFMailComposeViewControllerDelegate>

@property (nonatomic, readonly) UITableView * tableView;
@property (nonatomic, readonly) UIBarButtonItem * shareAction;

- (void)mailComposeController:(MFMailComposeViewController *)controller
          didFinishWithResult:(MFMailComposeResult)result
                        error:(NSError *)error;

@end
