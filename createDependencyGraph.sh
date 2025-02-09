#!/bin/bash
module=$1
result_file=${module//\//_}

mvn com.github.ferstl:depgraph-maven-plugin:aggregate -DDshowGroupIds -f $module

input_file=$module/target/dependency-graph.dot

# do some cleanup
mkdir -p target
cleared_file=target/${result_file}.dot
cat $input_file | sed "s/:jar:bundle:/:/g" | sed "s/:bundle:/:/g" | sed "s/:jar:/:/g" | sed "s/:provided\"/\"/g" | sed "s/:compile\"/\"/g" | sed "s/:test\"/\"/g" > $cleared_file

dest_file=target/${result_file}.png
dot -Tpng $cleared_file -o $dest_file

echo "Image generated in $dest_file"
