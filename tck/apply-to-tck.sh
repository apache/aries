# Fill in the variable below, e.g. OSGI_GIT_DIR=/Users/david/clones/osgi-build
OSGI_GIT_DIR=...the root of your OSGi build clone...

# Updated the BSN of the implemetation bundle to org.apache.aries.spifly.dynamic as on some Mac OSX having the bundle suffix
# causes confusion
rm META-INF/MANIFEST.MF
cp ../spi-fly-dynamic-bundle/target/org.apache.aries.spifly.dynamic.bundle-0.4-SNAPSHOT.jar org.apache.aries.spifly.dynamic-0.4.0.jar
jar xvf org.apache.aries.spifly.dynamic-0.4.0.jar META-INF/MANIFEST.MF
cat META-INF/MANIFEST.MF | sed s/org.apache.aries.spifly.dynamic.bundle/org.apache.aries.spifly.dynamic/ > UPDATED_MANIFEST.MF
jar uvfm org.apache.aries.spifly.dynamic-0.4.0.jar UPDATED_MANIFEST.MF
cp org.apache.aries.spifly.dynamic-0.4.0.jar $OSGI_GIT_DIR/licensed/repo/org.apache.aries.spifly.dynamic
