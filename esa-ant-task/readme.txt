# esa-ant
This ant taskdef will help you to generate the OSGI Enterprise Archive (*.esa) bundles. This could be used in traditional ant taskdef way in to your build scripts

# Getting started
Clone the project and then run `mvn clean install`, grab the jar and drop it your ant classpath typically ~/.ant/lib , or $ANT_HOME/lib etc.,

# Sample build file

```xml

<?xml version="1.0" encoding="UTF-8"?>
<!-- ====================================================================== 
                                                    

     An ant example for esa-ant    
     A simple build file to demonstrate the use of esa-ant task
                   
     kameshs                                                                
     ====================================================================== -->
<project name="An ant example for esa-ant" default="default">
	<description>
            A simple build file to demonstrate the use of esa-ant task
    </description>

	<taskdef name="esa" classname="org.apache.aries.ant.taskdefs.EsaTask" />

	<target name="default" description="builds esa with supplied SUBSYSTEM.MF">
		<esa destfile="demo.esa" symbolicname="test-esa" manifest="${basedir}/SUBSYSTEM.MF">
			<fileset dir="/tmp/esa-ant-demo">
				<include name="*.jar" />
			</fileset>
		</esa>
	</target>

	<target name="default2" description="generates the SUSBYSTEM.MF based on esa contents">
		<esa destfile="demo2.esa" symbolicname="test-esa" generatemanifest="true">
			<fileset dir="/tmp/esa-ant-demo">
				<include name="*.jar" />
			</fileset>
		</esa>
	</target>


</project>
```