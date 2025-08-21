# API
The plugin exposes an API endpoint to retrieve information on the configured clouds in JSON format.

The endpoint is available by making a GET request to `https://<jenkins-url>/ec2-fleet/stats`

The response payload will be:
```
[ 
    {
        "numActive": 0,
        "fleet": "fleet-1",
        "numDesired": 0,
        "state": "active",
        "label": "linux"
    } 
]
```