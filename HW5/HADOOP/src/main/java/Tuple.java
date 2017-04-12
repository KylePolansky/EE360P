
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import org.apache.hadoop.io.Writable;

public class Tuple implements Writable {
    String word;
    int count;

    public Tuple() {
    }
    
    public Tuple(String word, int count) {
        this.word = word;
        this.count = count;
    }
    
    public Tuple(Tuple t) {
        this.word = t.getWord();
        this.count = t.getCount();
    }
    
    public void increment(int n) {
        count+=n;
    }
   

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object obj) {
        return ((Tuple)obj).getWord().equals(word);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.word);
        return hash;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(word);
        out.writeInt(count);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        word=in.readUTF();
        count=in.readInt();
    }
    
    
    
}
