#!/bin/bash

while read -r line; do
  echo "$line"
  #newpath=`echo "$line" | sed 's/java\/com\/lorainelab/java\/org\/lorainelab\/igb/g'`
  #echo $newpath
  #mkdir -p "$newpath"
 # mv "$line" "$newpath"
done < <(find -follow | grep "src/main/java/org/lorainelab/igb/lorainelab$")
