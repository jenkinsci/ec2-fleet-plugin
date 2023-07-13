# Windows Agent with EC2 Fleet Plugin

This guide describes how to configure Windows EC2 Instance to be good for run
as Agent for EC2 Fleet Jenkins Plugin. At the end of this guide you 
will get AWS EC2 AMI (Image) which could be used for Auto Scaling Group
or EC2 Spot Fleet to run Windows agents.

**Big thanks to @Michenux for help to find all details**

**Note** Before this, please consider to use Windows OpenSSH 
https://github.com/jenkinsci/ssh-slaves-plugin/blob/master/doc/CONFIGURE.md#launch-windows-slaves-using-microsoft-openssh

**Note** This guide uses Windows DCOM technology (not open ssh) it doesn't work over NAT,
so Jenkins Master EC2 Instance should be placed in same VPC as Agents managed by EC2 Fleet Plugin.

## Run EC2 Instance with Windows

1. Note Windows Password for this guide
1. Login to Windows

## Create Jenkins User

1. Goto ```Local Users and Groups```
1. Click ```Users```
1. Create New with name ```jenkins```
  - Set password and note it
  - Set ```Password never expires```
  - Set ```User cannot change password```
  - Unset ```User must change password at next logon```
1. Goto user properties, find ```Member Of``` add ```Administrators``` group

## Login to Windows as jenkins user

### Configure Windows Registry

1. Run ```regedit```

1. Set ```HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\FileSystem\LongPathsEnabled``` to ```1```

1. Goto ```HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System```
1. Create/Modify ```DWORD-32``` with name ```LocalAccountTokenFilterPolicy``` value ```1```

1. Goto ```HKEY_LOCAL_MACHINE\System\CurrentControlSet\Control\Lsa```
1. Create/Modify ```DWORD-32``` with name ```LMCompatibilityLevel``` value ```2```
   - send NTLM authentication only
   
1. Find key ```76A64158-CB41-11D1-8B02-00600806D9B6``` 
   - it’s in ```HKEY_CLASSES_ROOT\CLSID```
1. Right click and select ```Permissions```
1. Change owner to ```Administrators``` select apply to children
1. Add ```Full Control``` to ```Administrators``` make sure to apply for children as well
1. Change owner back to ```NT Service\TrustedInstaller``` select apply to children

1. Run service ```Remote Registry```
1. Restart Windows

### Configure smb

1. Run as ```PowerShell``` as Administrator
1. Run ```Enable-WindowsOptionalFeature -Online -FeatureName smb1protocol```
1. Run ```Set-SmbServerConfiguration -EnableSMB1Protocol $true```

### Configure Firewall

1. Search for ```Windows Defender Firewall```
1. Click ```Advanced settings```
1. Goto ```Inbound Rules```
1. Add ```Remote Assistance TCP 135```
1. Add ```File and Printer Sharing (NB-Name-In) UDP 137```
1. Add ```File and Printer Sharing (NB-Datagram-In) UDP 138```
1. Add ```File and Printer Sharing (NB-Session-In) TCP 139```
1. Add ```File and Printer Sharing (SMB-In) TCP 445```
1. Add ```jenkins-master 40000-60000 TCP 40000-60000```
1. Add ```Administrator at Distance COM+ (DCOM) TCP C:\WINDOWS\System32\dllhost.exe```
1. For all created goto ```Properties -> Advanced``` and set ```Allow edge traversal```

## Install Java

1. Open ```PowerShell```
1. Install [Scoop](https://scoop.sh/) ```Invoke-Expression (New-Object System.Net.WebClient).DownloadString('https://get.scoop.sh')```
```scoop install git-with-openssh```
1. ```scoop bucket add java```
1. ```scoop install ojdkbuild8-full```

### Configure System Path for Java

1. Goto ```Control Panel\System and Security\System```
1. Goto ```Advanced System Settings```
1. Goto ```Environment Variables...```
1. Add Java Path (```C:\Users\jenkins\scoop\apps\ojdkbuild8-full\current\bin``` installed before by scoop) to System ```PATH```

## Create EC2 AMI

1. Goto to AWS Console and create image of preconfigured instance

## Before using this AMI for Jenkins Agent

- Make sure you required traffic could go to Windows from Jenkins. You can find
required ports above in ```Configure Firewall``` section

## Troubleshooting 

- https://github.com/jenkinsci/windows-slaves-plugin/blob/35b7f1d77b612af2c45b558b03538d0fb53fc05b/docs/troubleshooting.adoc