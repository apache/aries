#!/bin/bash
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.

TAG_SEARCH=spifly*
WORK_DIR=.
CHANGELOG=${WORK_DIR}/changelog.md

##########################################################################################
############# Do not edit below ##########################################################
##########################################################################################

while (( "$#" )); do
	case "$1" in
		-u|--update-changelog)
			if [ -n "$2" ] && [ ${2:0:1} != "-" ]; then
				UPDATE=$2
				shift 2
			else
				echo "Error: Tag for $1 is missing" >&2
				exit 1
			fi
			;;
		-*|--*=) # unsupported flags
			echo "Error: Unsupported flag $1" >&2
			exit 1
			;;
		*) # preserve positional arguments
			shift
			;;
	esac
done

# collect all the tags that match our pattern and reverse the order
ALL_TAGS=( $(git tag -l $TAG_SEARCH | xargs -n 1 | tac) )
PREVIOUS_TAG="${ALL_TAGS[0]}"
TAGS=${ALL_TAGS[@]:1}

if [ "$UPDATEx" != "x" ]; then
	printf "Updating $CHANGELOG\n"
	# update the file if asked (appends to head of file)

	# read the first tag in the existing changelog
	line=$(head -n 1 $CHANGELOG)
	TAGS=("${line#\#\# }")

	# set the previous tag to the passed argument
	PREVIOUS_TAG=$UPDATE
	printf "Collecting changes from $TAGS to $PREVIOUS_TAG\n"

	# swap the changelog for a temp file
	ORIGINAL_FILE=$CHANGELOG
	CHANGELOG="$CHANGELOG.tmp"
fi

# create the file if it doesn't exist
printf "" > $CHANGELOG

# write the changelog
for tag in $TAGS; do
	RESULT=$(git --no-pager shortlog --no-merges --grep 'maven-release-plugin' --invert-grep --format="- %s [%h]" -w0,2 -nec $tag..$PREVIOUS_TAG $WORK_DIR)

	if [[ ! -z "${RESULT// }" ]]; then
			echo "## $PREVIOUS_TAG" >> $CHANGELOG
			echo "$RESULT" >> $CHANGELOG
			echo "" >> $CHANGELOG
	fi

	PREVIOUS_TAG=$tag
done

if [ "$UPDATEx" != "x" ]; then
	# append the changelog to the end of temp
	cat $ORIGINAL_FILE >> $CHANGELOG

	# move the changelog temp over the changelog
	mv -f $CHANGELOG $ORIGINAL_FILE
fi
