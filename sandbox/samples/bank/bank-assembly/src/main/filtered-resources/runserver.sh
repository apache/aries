cp ../../bank-api/target/bank-api-${project.version}.jar .
cp ../../bank-chequingAccount/target/chequingAccount-${project.version}.jar .
cp ../../bank-chequingAccountBindings/target/chequingAccountBindings-${project.version}.jar .
cp ../../bank-creditCheck/target/creditCheck-${project.version}.jar .
cp ../../bank-creditCheckBindings/target/creditCheckBindings-${project.version}.jar .
cp ../../bank-lineOfCreditAccount/target/lineOfCreditAccount-${project.version}.jar .
cp ../../bank-lineOfCreditAccountBindings/target/lineOfCreditAccountBindings-${project.version}.jar .
java -Dorg.osgi.sca.domain.registry=tribes:default -jar osgi-3.5.0.v20090520.jar -configuration serverConfig/ -console
