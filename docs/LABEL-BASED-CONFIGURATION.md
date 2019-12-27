[Back to README](../README.md)

# Label Based Configuration

* [Overview](#overview)
* [How it works](#how-it-works)
* [Supported Parameters](#supported-parameters)
* [Configuration](#configuration)

# Overview

Feature in *beta* mode. Please report all problem [here](https://github.com/jenkinsci/ec2-fleet-plugin/issues/new)

This feature auto manages EC2 Spot Fleet or ASG based Fleets for Jenkins based on 
label attached to Jenkins Jobs.

With this feature user of EC2 Fleet Plugin doesn't need to have pre-created AWS resources 
to start configuration and run Jobs. Plugin required just AWS Credentials 
with permissions to be able create resources.

# How It Works

- Plugin detects all labeled Jobs where Label starts from Name configured in plugin configuration ```Cloud Name```
- Plugin parses Label to get Fleet configuration
- Plugin creates dedicated fleet for each unique Label
  - Plugin uses [CloudFormation Stacks](https://aws.amazon.com/cloudformation/) to provision Fleet and all required resources
- When Label is not used by any Job Plugin deletes Stack and release resources  

Label format
```
<CloudName>_parameter1=value1,parameter2=value2
```

# Supported Parameters

*Note* Parameter name is case insensitive

| Parameter | Value Example | Value |      
| --- | ---| ---- |
| imageId | ```ami-0080e4c5bc078760e``` | *Required* AMI ID https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html |
| max | ```12``` | Fleet Max Size, positive value or zero. If not specified plugin configuration Max will be used |
| min | ```1``` | Fleet Min Size, positive value or zero. If not specified plugin configuration Min will be used |
| instanceType | ```c4.large``` | EC2 Instance Type https://aws.amazon.com/ec2/instance-types/. If not specified ```m4.large``` will be used |
| spotPrice | ```0.4``` | Max Spot Price, if not specified EC2 Spot Fleet API will use default price. |

### Examples

Minimum configuration just Image ID
```
<FleetName>_imageId=ami-0080e4c5bc078760e
```

# Configuration

1. Create AWS User
1. Add Inline User Permissions
```json
{
    "Version": "2012-10-17",
    "Statement": [{
        "Effect": "Allow",
        "Action": [
            "cloudformation:*",
            "ec2:*",
            "autoscaling:*",
            "iam:ListRoles",
            "iam:PassRole",
            "iam:ListInstanceProfiles",
            "iam:CreateRole",
            "iam:AttachRolePolicy",
            "iam:GetRole"
        ],
        "Resource": "*"
    }]
}
```
1. Goto ```Manage Jenkins > Configure Jenkins```
1. Add Cloud ```Amazon EC2 Fleet label based```
1. Specify ```AWS Credentials```
1. Specify ```SSH Credentials```
   - Jenkins need to be able to connect to EC2 Instances to run Jobs
1. Set ```Region```
1. Provide base configuration
   - Note ```Cloud Name```
1. Goto to Jenkins Job which you want to run on this Fleet
  1. Goto Job ```Configuration```
  1. Enable ```Restrict where this project can be run```
  1. Set Label value to ```<Cloud Name>_parameterName=paremeterValue,p2=v2``` 
  1. Click ```Save```
  
In some short time plugin will detect Job and will create required resources to be able 
run it in future.  
  
That's all, you can repeat this for other Jobs.
