Installation instructions on Karaf 4

# Install DB and create DataSource service
feature:repo-add pax-jdbc 0.6.0
feature:install jdbc pax-jdbc-h2 pax-jdbc-config pax-jdbc-pool-dbcp2
jdbc:ds-create -dn H2-pool-xa -url jdbc:h2:mem:tasklist tasklist

# Install hibernate, aries jpa and example
feature:install scr jpa hibernate/4.3.6.Final http-whiteboard
install -s mvn:org.apache.aries.jpa.example/org.apache.aries.jpa.example.tasklist.model/2.0.0
install -s mvn:org.apache.aries.jpa.example/org.apache.aries.jpa.example.tasklist.ds/2.0.0

# This should show three active DS components
scr:list

# This should show the TaskService
service:list TaskService

# This should show the TasklistServlet
http:list


# Now open the url 
http://localhost:8181/tasklist
# You should see one task named Task1
 