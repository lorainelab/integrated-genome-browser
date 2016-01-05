#!/bin/bash

while read -r line; do
  echo "$line"
  newpath=`echo "$line" | sed 's/java\/org\/lorainelab\/igb\/igb/java\/org\/lorainelab\/igb/g'`
  echo $newpath
  mv "$line" /tmp/staging
  cp -R /tmp/staging/* $newpath
  rm -rf /tmp/staging
  #mkdir -p "$newpath"
 # mv "$line" "$newpath"
done < <(find -follow | grep "src/main/java/org/lorainelab/igb/igb$")
