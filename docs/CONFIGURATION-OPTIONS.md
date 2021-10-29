# EC2 Fleet

Parameter | Description | Default
--- | --- | ---
Name | The name of the cloud for Jenkins use. | FleetCloud
AWS Credentials | AWS Credentials used for connecting to instances, provisioning new ones, etc. | 
Region | AWS region the Fleet or ASG will reside in. | ap-east-1 (first item in alphabetical list)
Endpoint | AWS regional endpoint. Optional field only necessary if the desired region is not present in the region dropdown menu. | 
EC2 Fleet | Spot Fleet or ASG used for this Jenkins cloud. | 
Show all fleets | Show all Spot fleets in the region, not just the ones that are active and use maintain target capacity. | disabled 
Launcher | Options for connecting to new instances and creating Jenkins nodes from them. | 
Private IP | Connect to an instance using its private ip address instead of its public ip address. | disabled (use public IP)
Always Reconnect | Reconnect to offline nodes unless they've been terminated. | disabled
Restrict usage | Only allow jobs that have a matching label to use this fleet. If unchecked, the fleet will be considered for any pending job. | disabled
Label | The string value of the label that will restrict job placement. | <blank>
Jenkins Filesystem Root | Location of the Jenkins Filesystem Root. If left blank, a new directory will be made under the default directory. | `/tmp/jenkins-<RANDOM UUID>`
Number of Executors | The number of executors per instance. Changing the number of executors will only affect future instances and existing instances (if any) will remain unaffected. | 1
Scale Executors by Weight | Multiply the number of executors on an instance by the weight of the instance in the Spot Fleet. Good for Spot Fleets with instances of various sizes. | disabled
Max Idle Minutes Before Scaledown | After this amount of time an idle instance will be scheduled for termination. **If set to 0, instances will never be terminated**. | 0
Minimum Cluster Size | The lower limit on the number of instances the fleet can have. Uses instance weight, when applicable. Should be set equal to the ASG or Spot Fleet max instances. | 1
Maximum Cluster Size | The upper limit on the number of instances the fleet can have. Uses instance weight, when applicable. Should be set equal to the ASG or Spot Fleet max instances. | 1
Maximum Total Uses | Set a maximum total uses allowed for instances. After running 'maximum total uses' amount of jobs, the instance would be terminated. Set -1 for unlimited | -1 
Disable Build Resubmit | Do not automatically resubmit jobs that were interrupted due to an instance termination (manual termination, Spot interruption, etc.) | disabled
Maximum Init Connection Timeout in sec | EC2 instances aren't ready immediately after they're provisioned. They must become active and complete any userdata script. If that process takes longer than the time set here, consider that EC2 instance lost. | 180
Cloud Status Interval in sec | How long to wait between update cycles. Shorter times enable the fleet to scale faster, but cause more API calls. | 10
No Delay Provision Strategy | The default Jenkins strategy scales exponentially, meaning it might take a few cycles before all the pending jobs are provisioned. The "No Delay Provisioning Strategy" tries to get enough executors for all pending jobs in a single cycle. | disabled


# EC2 Fleet Label Based

Parameter | Description | Default
--- | --- | ---
Name | The name of the cloud for Jenkins use. | FleetCloudLabel
AWS Credentials | AWS Credentials used for connecting to instances, provisioning new ones, etc. | 
Region | AWS region the Fleet or ASG will reside in. | ap-east-1 (first item in alphabetical list)
Endpoint | AWS regional endpoint. Optional field only necessary if the desired region is not present in the region dropdown menu. | 
EC2 Key Name | The name of the SSH Key Pair used for new instances | 
Launcher | Options for connecting to new instances and creating Jenkins nodes from them. | 
Private IP | Connect to an instance using its private ip address instead of its public ip address. | disabled (use public IP)
Always Reconnect | Reconnect to offline nodes unless they've been terminated. | disabled
Restrict usage | Only allow jobs that have a matching label to use this fleet. If unchecked, the fleet will be considered for any pending job. | disabled
Jenkins Filesystem Root | Location of the Jenkins Filesystem Root. If left blank, a new directory will be made under the default directory. | `/tmp/jenkins-<RANDOM UUID>`
Number of Executors | The number of executors per instance. Changing the number of executors will only affect future instances and existing instances (if any) will remain unaffected. | 1
Max Idle Minutes Before Scaledown | After this amount of time an idle instance will be scheduled for termination. **If set to 0, instances will never be terminated**. | 0
Minimum Cluster Size | The lower limit on the number of instances the fleet can have. Uses instance weight, when applicable. Should be set equal to the ASG or Spot Fleet max instances. | 1
Maximum Cluster Size | The upper limit on the number of instances the fleet can have. Uses instance weight, when applicable. Should be set equal to the ASG or Spot Fleet max instances. | 1
Disable Build Resubmit | Do not automatically resubmit jobs that were interrupted due to an instance termination (manual termination, Spot interruption, etc.) | disabled
Maximum Init Connection Timeout in sec | EC2 instances aren't ready immediately after they're provisioned. They must become active and complete any userdata script. If that process takes longer than the time set here, consider that EC2 instance lost. | 180
Cloud Status Interval in sec | How long to wait between update cycles. Shorter times enable the fleet to scale faster, but cause more API calls. | 10
No Delay Provision Strategy | The default Jenkins strategy scales exponentially, meaning it might take a few cycles before all the pending jobs are provisioned. The "No Delay Provisioning Strategy" tries to get enough executors for all pending jobs in a single cycle. | disabled
