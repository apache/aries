# This script modifies the SPI-Fly bundle metadata slightly so that it can be copied into the OSGi CT. 
# It also copies the jar in place.

# Fill in the variable below, e.g. OSGI_GIT_DIR=/Users/david/clones/osgi-build
OSGI_GIT_DIR=...the root of your OSGi build clone...
VERSION=1.0.0

# Updated the BSN of the implemetation bundle to org.apache.aries.spifly.dynamic as on some Mac OSX having the bundle suffix
# causes confusion
rm META-INF/MANIFEST.MF
cp ../spi-fly-dynamic-bundle/target/org.apache.aries.spifly.dynamic.bundle-$VERSION-SNAPSHOT.jar org.apache.aries.spifly.dynamic-$VERSION.jar
jar xvf org.apache.aries.spifly.dynamic-$VERSION.jar META-INF/MANIFEST.MF
cat META-INF/MANIFEST.MF | sed s/org.apache.aries.spifly.dynamic.bundle/org.apache.aries.spifly.dynamic/ > UPDATED_MANIFEST.MF
jar uvfm org.apache.aries.spifly.dynamic-$VERSION.jar UPDATED_MANIFEST.MF
cp org.apache.aries.spifly.dynamic-$VERSION.jar $OSGI_GIT_DIR/licensed/repo/org.apache.aries.spifly.dynamic
