package services

import org.json.JSONObject
import org.kissweb.database.Connection
import org.kissweb.rest.GroovyService
import org.kissweb.rest.ProcessServlet
import org.kissweb.rest.MainServlet

class MyGroovyService {

    void addNumbers(JSONObject injson, JSONObject outjson, Connection db, ProcessServlet servlet) {
        int num1 = injson.getInt("num1")
        int num2 = injson.getInt("num2")
        /*
             We could perform all of the processing here.  Or, we can call a Groovy script as follows.
             Keep in mind that this is a compiled and cached script so it runs as fast as regular compiled Java.
             It gets auto-recompiled and loaded whenever it changes.
         */
        Integer r = GroovyService.run(false, "~/scripts", "MyScript", "myMethod", num1, num2)
        outjson.put("num3", (int) r)
    }

    void hasDatabase(JSONObject injson, JSONObject outjson, Connection db, ProcessServlet servlet) {
        outjson.put("hasDatabase", MainServlet.hasDatabase())
    }

}