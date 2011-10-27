cp ../../bank-api/target/bank-api-${project.version}.jar .
cp ../../bank-web/target/bank-web-${project.version}.jar .
cp ../../bank-biz/target/bank-biz-${project.version}.jar .
java -Dorg.osgi.sca.domain.registry=tribes:default -jar osgi-3.5.0.v20090520.jar -configuration clientConfig/ -console
