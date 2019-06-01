# ec2-spot-jenkins-plugin

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/ec2-fleet-plugin/master)](https://ci.jenkins.io/blue/organizations/jenkins/Plugins%2Fec2-fleet-plugin/activity) [![](https://img.shields.io/jenkins/plugin/v/ec2-fleet.svg)](https://github.com/jenkinsci/ec2-fleet-plugin/releases)

The EC2 Spot Jenkins plugin launches EC2 Spot instances as worker nodes for Jenkins CI server, 
automatically scaling the capacity with the load. 

* [Jenkins Page](https://wiki.jenkins.io/display/JENKINS/Amazon+EC2+Fleet+Plugin)
* [Report Issue](https://github.com/jenkinsci/ec2-fleet-plugin/issues/new)
* [Release Notes](https://github.com/jenkinsci/ec2-fleet-plugin/releases)
* [Overview](#overview)
* [Usage](#usage)
* [Development](#development)

# Overview
This plugin uses Spot Fleet to launch instances instead of directly launching them by itself. 
Amazon EC2 attempts to maintain your Spot fleet's target capacity as Spot prices change to maintain
the fleet within the specified price range. For more information, see 
[How Spot Fleet Works](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet.html).

# Usage

## Setup

#### 1. Get AWS Account

[AWS account](http://aws.amazon.com/ec2/)

#### 2. Create IAM User

Specify ```programmatic access``` during creation, and record credentials 
which will be used by Jenkins EC2 Fleet Plugin to connect to your Spot Fleet
 
#### 3. Configure User permissions

Add inline policy to the user to allow it use EC2 Spot Fleet 
[AWS documentation about that](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet-requests.html#spot-fleet-prerequisites)

```json
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Effect": "Allow",
              "Action": [
                  "ec2:*"
              ],
              "Resource": "*"
          },
          {
              "Effect": "Allow",
              "Action": [
                "iam:ListRoles",
                "iam:PassRole",
                "iam:ListInstanceProfiles"
              ],
              "Resource": "*"
          }
      ]
  }
```

#### 4. Create EC2 Spot Fleet

https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet-requests.html#create-spot-fleet

Make sure that you specify an SSH key that will be used later by Jenkins.

#### 5. Configure Jenkins

Once the fleet is launched, you can set it up by adding a new **EC2 Fleet** cloud in the Jenkins

1. Goto ```Manage Jenkins > Plugin Manager``` 
1. Install ```EC2 Fleet Jenkins Plugin```
1. Goto ```Manage Jenkins > Configure System```
1. Click ```Add a new cloud``` and select ```Amazon SpotFleet```
1. Configure credentials and specify EC2 Spot Fleet which you want to use

## Scaling
You can specify the scaling limits in your cloud settings. By default, Jenkins will try to scale fleet up
if there are enough tasks waiting in the build queue and scale down idle nodes after a specified idleness period.

You can use the History tab in the AWS console to view the scaling history. 

# Development

## Releasing

https://jenkins.io/doc/developer/publishing/releasing/

```bash
mvn release:prepare release:perform
```
