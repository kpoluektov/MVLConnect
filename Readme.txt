Another one try to write Oracle source connector.

In general work over Oracle materialized log. So doesn't require any additional licensing except DB itself.
To start using just
- clone, compile and package (fat jar) the project:
    sbt "set test in assembly := {}" clean assembly
- create materialized view log on target table(s)
    CREATE MATERIALIZED VIEW LOG ON customers WITH PRIMARY KEY, COMMIT SCN;
- customize settings
    connect-distributed.properties
        bootstrap.servers=
        group.id=
    MVLConnector.properties
        tasks.max=2                                         # connection tasks parallel level
        topic=pol_test                                      #
        orcl.url=jdbc:oracle:thin:@localhost:1521/xe        #
        orcl.driver=oracle.jdbc.OracleDriver                #
        orcl.user=pol                                       #
        orcl.password=pol                                   #
        orcl.connectionPool = disabled                      #
        orcl.numThreads = 10                                #
        orcl.keepAliveConnection = true                     #
        orcl.schema = pol                                   #
        tables=POL.CUSTOMERS, POL.DEPARTS                   # tables to capture changes
        processChanges=false                                #
        flashbackQuery=true                                 # to capture final/in time (aka "as of scn") state of record
        timeout=10                                          #
        start.scn=0                                         # start scn
- additionally add 'read' right to oracle user for reading current_scn
    grant read on v_$database to pol;
- start connector
    connect-standalone.sh /usr/bin/connect-standalone.properties /usr/bin/MVLConnector.properties
    or use docker-compose
- create tasks
    curl -X POST -H "Content-Type: application/json" --data @config.json http://localhost:8083/connectors


restrictions:
    - doesn't support
        - multicolumn primary key;
        - changes for tables with LOB/UDT fields;
        - and lots of others, to be fixed in the future;


