package model;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.steadystate.css.parser.CSSOMParser;
import org.junit.Test;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleSheet;
import jdk.nashorn.api.tree.*;


/**
 * Created by Nathan on 22/11/2017.
 */
public class Model extends java.util.Observable {

    private CSVManager csvManager = new CSVManager();

    private ArrayList<Assignment> assignments = new ArrayList<>();
    private ArrayList<Unmarkable> unmarkables = new ArrayList<>();
    private List<Testable> currentTests = new ArrayList<>();

    /***
     * Parses the html, css and javascript. Invalid sections are ignored ie: invalid values for css properties.
     * @param folders
     */
    public void loadFiles(File[] folders) {
        closeFiles();
        for (int i = 0; i < folders.length; i++) {
            ArrayList<Document> htmlDocs = new ArrayList<>();
            ArrayList<CSSStyleSheet> cssDocs = new ArrayList<>();
            ArrayList<CompilationUnitTree> javascriptDocs = new ArrayList();

            for (final File file : folders[i].listFiles()) {
                String fileName = file.getName().toLowerCase();
                try {
                    if (fileName.endsWith(".html")) {
                        htmlDocs.add(Jsoup.parse(file, "UTF-8", file.getName()));
                    } else if (fileName.endsWith(".css")) {
                        cssDocs.add(parseCss(file));
                    } else if (fileName.endsWith(".js")) {
                        Parser parser = Parser.create();
                        CompilationUnitTree tree = parser.parse(file,null);
                        javascriptDocs.add(tree);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    //TODO add something to log, maybe just print to a visible console
                }
                //TODO add some error/log info if there css/html/javascript is null;
            }

            String unmarkableString = checkValid(htmlDocs, cssDocs, javascriptDocs);
            if(unmarkableString.equals("")){
                //Only one css doc and js file for each assignment
                if(!javascriptDocs.isEmpty()) {
                    assignments.add(new Assignment(folders[i].getName(), htmlDocs, cssDocs.get(0), javascriptDocs.get(0)));
                }else{
                    Parser parser = Parser.create();
                    CompilationUnitTree nullTree = parser.parse("emptyFile", "" ,null);
                    assignments.add(new Assignment(folders[i].getName(), htmlDocs, cssDocs.get(0), nullTree));
                }
            }else{
                unmarkables.add(new Unmarkable(folders[i].getName(), unmarkableString));
            }
        }

        setChanged();
        notifyObservers();

    }

    /**
     * Removes the currently loaded in files/documents
     */
    public void closeFiles() {
        this.assignments = new ArrayList<>();
        this.currentTests = new ArrayList<>();
        this.unmarkables = new ArrayList<>();
        setChanged();
        notifyObservers();
    }

    /**
     * Runs the tests, updating the assignments.
     * @param tests
     * @param percentages Each percentage corresponds to a test.
     * @throws Exception
     */
    public void runTests(List<Testable> tests, ArrayList<Double> percentages) throws Exception {
        clearResults();
        currentTests = tests;
        for(Assignment assignment: assignments){
            for(int i = 0; i < tests.size(); i++){
                assignment.addResults(currentTests.get(i).runTest(assignment.getHtmlDocs(), assignment.getCssDocs(), assignment.getJavaScriptDocs(), percentages.get(i)));
            }
        }
        setChanged();
        notifyObservers();
    }

    public Testable[] getAvailableTests() {
        ArrayList<Testable> availableTests = TestManager.getTests();
        return availableTests.toArray(new Testable[availableTests.size()]);
    }

    public List<Testable> getCurrentTests() {
        return currentTests;
    }

    public ArrayList<Assignment> getAssignments() {
        return assignments;
    }

    public ArrayList<Unmarkable> getUnmarkables() {
        return unmarkables;
    }

    public void loadCSV(File file){
        csvManager.loadCsv(file);
        setChanged();
        notifyObservers();
    }

    public String[] getColumns(){
        return csvManager.getColumns();
    }

    public void createCSV(){
        csvManager.createCSV();
    }

    private CSSStyleSheet parseCss(File file) throws IOException {
        InputStream stream = new FileInputStream(file);
        InputSource source = new InputSource(new InputStreamReader(stream));
        CSSOMParser parser = new CSSOMParser();
        CSSStyleSheet stylesheet = parser.parseStyleSheet(source, null, null);  //These can be stored
        return stylesheet;
    }

    /***
     * Checks whether a assignment is valid
     * @param htmlDocs
     * @param cssDocs
     * @param javascriptDocs
     * @return A string containing the reason why the assignment is invalid based on documents. An empty string will return otherwise
     */
    private String checkValid(ArrayList<Document> htmlDocs, ArrayList<CSSStyleSheet> cssDocs, ArrayList<CompilationUnitTree> javascriptDocs){
        String reason = "";
        if(htmlDocs.isEmpty()) reason += "No html found, ";
        if(cssDocs.isEmpty()) reason += "No css found ,";
        if(cssDocs.size() > 1) reason += ">1 css document found";
        //if(javascriptDocs.isEmpty()) reason += "No javascript file found ,";
        //if(javascriptDocs.size() > 1) reason += ">1 javascript file found";
        return reason;
    }

    /***
     * Clears the test results for all held assignments.
     */
    private void clearResults(){
        for(Assignment assignment: assignments){
            assignment.clearResults();
        }
    }
}
