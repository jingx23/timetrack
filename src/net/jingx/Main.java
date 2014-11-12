package net.jingx;


import org.apache.commons.cli.*;

public class Main {

    public static void main(String[] args) {
        Options options = new Options();
        Option username = OptionBuilder.withArgName("username")
                .isRequired()
                .hasArg()
                .withDescription("username for login (required)")
                .create("u");

        Option password = OptionBuilder.withArgName("password")
                .isRequired()
                .hasArg()
                .withDescription("password for login (required)")
                .create("p");
        Option url = OptionBuilder.withArgName("url")
                .isRequired()
                .hasArg()
                .withDescription("jira url without trailing slash (required)")
                .create("url");
        Option maxResults = OptionBuilder.withArgName("maxResults")
                .isRequired(false)
                .hasArg()
                .withType(Number.class)
                .withDescription("maxResults to search in (default => last 100)")
                .create("maxResults");


        options.addOption(username);
        options.addOption(password);
        options.addOption(url);
        options.addOption(maxResults);


        CommandLineParser parser = new BasicParser();
        try {
            CommandLine line = parser.parse(options, args);
            System.out.println(new JiraParser(line.getOptionValue(url.getOpt()),
                            line.getOptionValue(username.getOpt()),
                            line.getOptionValue(password.getOpt()),
                            (Long)line.getParsedOptionValue(maxResults.getOpt()))
                            .parse()
                            .formattedString()
            );
        } catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("timetracker", options);
        }
    }
}
