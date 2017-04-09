

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
        
	public static class TextMapper extends Mapper<LongWritable, Text, Text, Tuple> {

		private Text contextWord = new Text();

		@Override
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
                        line=line.toLowerCase().replaceAll("[^a-z0-9]", " ");
			StringTokenizer tokenizer = new StringTokenizer(line);
                        HashSet<String> words = new HashSet();
                        while (tokenizer.hasMoreTokens()) {
                            words.add(tokenizer.nextToken());
                        }
                        
                        for(String word : words)
                        {
                            contextWord.set(word);
                            StringTokenizer tokenizer2 = new StringTokenizer(line);
                            boolean selfCounted=false;
                            while(tokenizer2.hasMoreTokens())
                            {
                                String queryWord = tokenizer2.nextToken();
                                if(!selfCounted && queryWord.equals(contextWord.toString()))
                                    selfCounted=true;
                                else{
                                    Tuple t = new Tuple(queryWord, 1);
                                    context.write(contextWord,t);
                                }
                            }
                        }
		}
	}
        
        private static Map<String,Tuple> collectWordMap(Iterable<Tuple> values){
            Map<String,Tuple> words = new TreeMap();
            for (Tuple value : values) {
                Tuple prevTuple = words.get(value.getWord());
                if(prevTuple==null)
                    prevTuple=new Tuple(value);
                else
                    prevTuple.increment(value.getCount());
                words.put(prevTuple.getWord(),prevTuple);
            }
            return words;
        }
        
        public static class TextCombiner extends Reducer<Text, Tuple, Text, Tuple> {

		@Override
		public void reduce(Text key, Iterable<Tuple> values, Context context)
				throws IOException, InterruptedException {
                    Map<String,Tuple> words = collectWordMap(values);
                    
                    for (Tuple t : words.values())
                    {
                        context.write(new Text(key), t);
                    }
                }
        }

	public static class TextReducer extends Reducer<Text, Tuple, Text, Text> {

		@Override
		public void reduce(Text key, Iterable<Tuple> values, Context context)
				throws IOException, InterruptedException {
			Map<String,Tuple> words = collectWordMap(values);
                        
                        StringBuilder resultSb = new StringBuilder();
                        for(Tuple t : words.values())
                        {
                            resultSb.append("\n<");
                            resultSb.append(t.getWord());
                            resultSb.append(", ");
                            resultSb.append(t.getCount());
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
            Job job = Job.getInstance(conf, "JC82563_KPP446"); // Replace with your EIDs
               
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
            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(Tuple.class);

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
