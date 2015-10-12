#IGB Command Socket

##Example Bash Script for looping through a list of genomic coordinates and exporting an image from the Integrated Genome Browser using the 'Command Socket' plugin

```bash
#!/bin/sh
# This script requires the 'Command Socket' plugin be enabled from the 'Plugins' tab
locationsOfInterest=('chr1:2,246,108-2,271,426' 'chr2:9,488,819-9,531,295' 'chr3:8,407,934-8,454,574' )

function igbCommand(){
  echo "$1" | ncat 127.0.0.1 7084
}

igbCommand 'genome A_thaliana_Jun_2009'

sleep 0.5

for i in "${locationsOfInterest[@]}"
do
	igbCommand 'goto '$i''
        igbCommand 'snapshotmainView /tmp/igbImage-'$i'.png'        
        sleep 2
done
```