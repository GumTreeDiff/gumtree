package com.github.gumtreediff.gen.c;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Pattern;

@Register(id = "c-cocci", accept = "\\.[ch]$")
public class CTreeGenerator extends TreeGenerator {

    private static final String COCCI_CMD = "cgum";

    private static final TreeContext.MetadataSerializers defaultSerializers = new TreeContext.MetadataSerializers();
    private static final TreeContext.MetadataUnserializers defaultUnserializers = new TreeContext.MetadataUnserializers();
    static {
        defaultSerializers.add("lines", x -> Arrays.toString((int[]) x));
        Pattern comma = Pattern.compile(", ");
        defaultUnserializers.add("lines", x -> {
            String[] v = comma.split(x.substring(1, x.length() - 2), 4);
            int[] ints = new int[v.length];
            for (int i = 0; i < ints.length; i++) {
                ints[i] = Integer.parseInt(v[i]);
            }
            return ints;
        });
    }

    @Override
    public TreeContext generate(Reader r) throws IOException {
        //FIXME this is not efficient but I am not sure how to speed up things here.
        File f = File.createTempFile("gumtree", ".c");
        System.out.println(f.getAbsolutePath());
        FileWriter w = new FileWriter(f);
        BufferedReader br = new BufferedReader(r);
        String line = br.readLine();
        while (line != null) {
            w.append(line);
            w.append(System.lineSeparator());
            line = br.readLine();
        }
        w.close();
        br.close();
        ProcessBuilder b = new ProcessBuilder(COCCI_CMD, f.getAbsolutePath());
        b.directory(f.getParentFile());
        try {
            Process p = b.start();
            StringBuffer buf = new StringBuffer();
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // TODO Why do we need to read and bufferize eveything, when we could/should only use generateFromStream
            line = null;
            while ((line = br.readLine()) != null)
                buf.append(line + "\n");
            p.waitFor();
            if (p.exitValue() != 0)  throw new RuntimeException();
            r.close();
            String xml = buf.toString();
            return TreeIoUtils.fromXml(CTreeGenerator.defaultUnserializers).generateFromString(xml);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            f.delete();
        }
    }
}
