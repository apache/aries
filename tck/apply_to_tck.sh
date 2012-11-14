# Change these
# Fill in the variable below, e.g. ARIES_DIR=/Users/david/checkouts/aries
ARIES_DIR=...the root of your apache aries checkout...
# Fill in the variable below, e.g. OSGI_GIT_DIR=/Users/david/clones/osgi-build
OSGI_GIT_DIR=...the root of your OSGi build clone...

JMX_TARGET_DIR=$OSGI_GIT_DIR/licensed/repo
#LIB_BASE=$OSGI_GIT_DIR/cnf/repo/org.apache.aries.impl.jmx
#LIB_FILE=$LIB_BASE/org.apache.aries.impl.jmx-4.2.0.lib

#mkdir -p -v $JMX_TARGET_DIR/org.apache.aries.jmx
cp $ARIES_DIR/sandbox/jmx-next/jmx-bundle/target/org.apache.aries.jmx-1.1.0-SNAPSHOT.jar $JMX_TARGET_DIR/org.apache.aries.jmx/org.apache.aries.jmx-1.1.0.jar

#mkdir -p -v $JMX_TARGET_DIR/org.apache.aries.util
cp $ARIES_DIR/util/util/target/org.apache.aries.util-1.0.0-SNAPSHOT.jar $JMX_TARGET_DIR/org.apache.aries.util/org.apache.aries.util-1.0.0.jar

# mkdir -p -v $LIB_BASE
# echo "slf4j.api; version=1.5.10" > $LIB_FILE
# echo "slf4j.simple; version=1.5.10" >> $LIB_FILE
# echo "org.apache.aries.util; version=$ARIES_TARGET_VERSION" >> $LIB_FILE
# echo "org.apache.aries.jmx; version=$ARIES_TARGET_VERSION" >> $LIB_FILE
# 
# cp $ARIES_DIR/sandbox/jmx-next/tck/org.osgi.test.cases.jmx.bnd.bnd $OSGI_GIT_DIR/org.osgi.test.cases.jmx/bnd.bnd

