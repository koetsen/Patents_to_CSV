import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;


import com.opencsv.CSVWriter;

public class App {


    public static void main(String[] args) {
        ArrayList<String> parentDirs = new ArrayList<>();
        ArrayList<Path> inFiles = new ArrayList<>();
        Writer csvOut;
        String outFile = "";

        if (SystemUtils.IS_OS_LINUX) {
            parentDirs.add("/home/koet/programmieren/patente");
            parentDirs.add("/home/koet/programmieren/patente/xml/patente_aufbereitet");
            outFile = "/home/koet/programmieren/patente/xml/patenCSV.csv";
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            parentDirs.add("C:\\zeug\\Programmieren\\xml-Patente\\patente_aufbereitet");
            parentDirs.add("C:\\zeug\\Programmieren\\patente");
            outFile = "C:\\zeug\\Programmieren\\csv\\patenCSV.csv";
        }

        if (parentDirs.isEmpty() || outFile.isEmpty()) {
            System.out.println("Da fehlen Parameter du Arsch!!");
            System.exit(0);
        }

        FilenameFilter patents = (File dir, String file) -> {
            if (file.endsWith(".xml")) {
                return true;
            } else
                return file.endsWith(".html");
        };

        // get Infiles
        for (String dir : parentDirs) {
            File inDir = new File(dir);
            for (String file : inDir.list(patents)) {
                inFiles.add(Paths.get(dir, file));
            }
        }

        try {
            csvOut = Files.newBufferedWriter(Paths.get(outFile), StandardCharsets.UTF_8);
            CSVWriter csvWriter = new CSVWriter(csvOut, '|', CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.NO_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
            csvWriter.writeNext(PatentHelper.getPatenFields());

            for (Path inPath : inFiles) {

                String fileExtension = FilenameUtils.getExtension(inPath.toString());
                if (fileExtension.equalsIgnoreCase("html")) {

                    System.out.printf("Bearbeite %s\n", inPath.getFileName().toString());
                    ParseGooglePatents patParser = new ParseGooglePatents(inPath.toFile());

                    csvOut(patParser, csvWriter);

                } else if (fileExtension.equalsIgnoreCase("xml")) {

                    System.out.printf("Analyse %s\n", inPath.getFileName().toString());
                    BufferedReader br = Files.newBufferedReader(inPath, PatentHelper.getCharsetFromFile(inPath));
                    XMLPatentsToCSV xml2csv = new XMLPatentsToCSV(br);
                    csvOut(xml2csv, csvWriter);
                }
            }

            System.out.println("Fettich");

            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void csvOut(CSV_Writable csvWR, CSVWriter wr) {
        csvWR.writeCSV(wr);
    }
}
