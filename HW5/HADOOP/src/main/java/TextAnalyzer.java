

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Example MapReduce program that performs word count.
 *
 * @author David Franke (dfranke@cs.utexas.edu)
 */
public class TextAnalyzer extends Configured implements Tool {
        private static final String SEPARATOR = ";";
        
	public static class TextMapper extends Mapper<LongWritable, Text, Text, Text> {

		private Text contextWord = new Text();
                private Text queryWord = new Text();

		@Override
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
                        line=line.toLowerCase().replaceAll("[^A-Za-z0-9]", " ");
			StringTokenizer tokenizer = new StringTokenizer(line);

			while (tokenizer.hasMoreTokens()) {
                            contextWord.set(tokenizer.nextToken());
                            StringTokenizer tokenizer2 = new StringTokenizer(line);
                            HashSet<String> wordSet = new HashSet();
                            while (tokenizer2.hasMoreTokens()) {
                                queryWord.set(tokenizer2.nextToken()+SEPARATOR+"1");
                                context.write(contextWord,queryWord);
                            }
			}
		}
	}
        
        private static Map<String,Integer> collectWordMap(Iterable<Text> values){
            TreeMap<String,Integer> words = new TreeMap();
            for (Text value : values) {
                String contextword = value.toString().split(SEPARATOR)[0];
                int nextCount = Integer.parseInt(value.toString().split(SEPARATOR)[1]);
                Integer count = words.get(contextword);
                if(count==null)
                    count=0;
                count+=nextCount;
                words.put(contextword, count);
            }
            return words;
        }
        
        public static class TextCombiner extends Reducer<Text, Text, Text, Text> {

		@Override
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
                    Map<String,Integer> words = collectWordMap(values);
                    
                    for (Entry<String,Integer> e : words.entrySet())
                    {
                        context.write(new Text(key), new Text(e.getKey()+SEPARATOR+e.getValue()));
                    }
                }
        }

	public static class TextReducer extends Reducer<Text, Text, Text, Text> {

		@Override
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			Map<String,Integer> words = collectWordMap(values);
                        
                        StringBuilder resultSb = new StringBuilder();
                        for(Entry<String,Integer> e : words.entrySet())
                        {
                            resultSb.append("\n<");
                            resultSb.append(e.getKey());
                            resultSb.append(", ");
                            resultSb.append(e.getValue());
                            resultSb.append(">");
                        }
                        resultSb.append("\n");
			// Emit the total count for the word.
			context.write(key, new Text(resultSb.toString()));
                        
		}
	}
        public int run(String[] args) throws Exception {
            Configuration conf = this.getConf();

            // Create job
            Job job = Job.getInstance(conf, "EID1_EID2"); // Replace with your EIDs
               
            //Had to add this line because my hadoop was giving an error
            job.getConfiguration().set("mapreduce.reduce.memory.mb","2048");

            job.setJarByClass(TextAnalyzer.class);
            

            // Setup MapReduce job
            job.setMapperClass(TextMapper.class);
            //   Uncomment the following line if you want to use Combiner class
            job.setCombinerClass(TextCombiner.class);
            job.setReducerClass(TextReducer.class);

            // Specify key / value types (Don't change them for the purpose of this assignment)
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);
            //   If your mapper and combiner's  output types are different from Text.class,
            //   then uncomment the following lines to specify the data types.
            //job.setMapOutputKeyClass(?.class);
            //job.setMapOutputValueClass(?.class);

            // Input
            FileInputFormat.addInputPath(job, new Path(args[0]));
            job.setInputFormatClass(TextInputFormat.class);

            // Output
            FileOutputFormat.setOutputPath(job, new Path(args[1]));
            job.setOutputFormatClass(TextOutputFormat.class);

            // Execute job and return status
            return job.waitForCompletion(true) ? 0 : 1;
        }
        
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new TextAnalyzer(), args);
                System.exit(res);
	}
}
