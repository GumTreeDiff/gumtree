package fr.labri.gumtree.client;

import fr.labri.gumtree.gen.Generators;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.io.TreeIoUtils.TreeSerializer;
import fr.labri.gumtree.tree.TreeContext;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;

@Register(name = "parse", description = "Parse file and dump result")
public class Serializer extends Client {

    public static final String SYNTAX = "Syntax: parse [options] file ...";

    static class Options implements Option.Context {
        protected OutputFormat format = OutputFormat.JSON;
        protected String output = null;
        protected String[] files;

        @Override
        public Option[] values() {
            return new Option[]{
                    new Option("-f", "Output format " + Arrays.toString(OutputFormat.values()), 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            OutputFormat o = OutputFormat.DOT.valueOf(args[0].toUpperCase());
                            if (o != null)
                                format = o;
                            else
                                System.err.println("Invalid output type: " + args[0]);
                        }
                    },
                    new Option("-o", "Output filename (or directory if more than one file), defaults to stdout", 1) {
                        @Override
                        protected void process(String name, String[] args) {

                        }
                    }
            };
        }
    }

    enum OutputFormat {
        JSON {
            @Override
            TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toJson(ctx);
            }
        },
        XML {
            @Override
            TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toCompactXml(ctx);
            }
        },
        FULLXML {
            @Override
            TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toXml(ctx);
            }
        },
        DOT {
            @Override
            TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toDot(ctx);
            }
        },
        LISP {
            @Override
            TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toLisp(ctx);
            }
        };

        abstract TreeSerializer getSerializer(TreeContext ctx);
    }

    Options opts = new Options();

    public Serializer(String[] args) {
        super(args);
        args = Option.processCommandLine(args, opts);
        if (args.length == 0)
            throw new Option.OptionException(SYNTAX);

        opts.files = args;
    }

    @Override
    public void run() throws IOException {
        final boolean multiple = opts.files.length > 1;
        if (multiple && opts.output != null)
            Files.createDirectories(FileSystems.getDefault().getPath(opts.output));

        for (String file : opts.files) {
            try {
                TreeContext tc = Generators.getInstance().getTree(file);
                opts.format.getSerializer(tc).writeTo(opts.output == null
                        ? System.out
                        : new FileOutputStream(opts.output));
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
}