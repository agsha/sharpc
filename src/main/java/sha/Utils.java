package sha;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by sharath.g on 09/06/15.
 */
public class Utils {

    public static final RandomString rs = new RandomString();
    private static final Logger log = LogManager.getLogger();

    public static Document readXml(String path) {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = builderFactory.newDocumentBuilder();
            return builder.parse(Files.newInputStream(Paths.get(path)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static Node evalXpathNode(Document doc, String exp) {
        XPath xPath =  XPathFactory.newInstance().newXPath();
        try {
            return (Node)xPath.compile(exp).evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public static Node evalXpathNode(Node node, String exp) {
        XPath xPath =  XPathFactory.newInstance().newXPath();
        try {
            return (Node)xPath.compile(exp).evaluate(node, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }


    public static NodeList evalXpathNodeList(Document doc, String exp) {
        XPath xPath =  XPathFactory.newInstance().newXPath();
        try {
            return (NodeList)xPath.compile(exp).evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }


    public static NodeList evalXpathNodeList(Node node, String exp) {
        XPath xPath =  XPathFactory.newInstance().newXPath();
        try {
            return (NodeList)xPath.compile(exp).evaluate(node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void walk(String path, String pattern, final FileVisitor<Path> fv) {
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
        try {
            Files.walkFileTree(Paths.get(path), new FileVisitor<Path>(){

                boolean ok(Path path) {
                    return matcher.matches(path);
                }
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if(!ok(dir)) return FileVisitResult.CONTINUE;
                    return fv.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(!ok(file)) return FileVisitResult.CONTINUE;
                    return fv.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    if(!ok(file)) return FileVisitResult.CONTINUE;
                    return fv.visitFileFailed(file, exc);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if(!ok(dir)) return FileVisitResult.CONTINUE;
                    return fv.postVisitDirectory(dir, exc);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static <T> T readJson(String path, Class<T> claz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(Files.readAllBytes(Paths.get(path)), claz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T loads(String json, Class<T> claz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, claz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readJson(String path, TypeReference tp) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(Files.readAllBytes(Paths.get(path)), tp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // returns a random int between min inclusive and max exclusive
    public static int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }
    public static <T> T readJsonFromClasspath(String path, TypeReference tp) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(ClassLoader.getSystemResourceAsStream(path), tp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readJsonFromClasspath(String path, Class<T> claz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(ClassLoader.getSystemResourceAsStream(path), claz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeJson(String path, Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String s = mapper.writeValueAsString(obj);
            Files.write(Paths.get(path), s.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String read(String fileName) {
        try {
            return new String(Files.readAllBytes(Paths.get(fileName)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> readLines(String fileName) {
        try {
            return Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readClasspathFile(String fileName) {
        try {
            return IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String prettifyJson(String json, ObjectMapper mapper) {
        try {
            Object o = mapper.readValue(json, Object.class);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void cc(String command) {
        try {
            new ProcessBuilder("/bin/bash", "-c", command).start().waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String prettifyJson(String json) {
        return prettifyJson(json, new ObjectMapper());
    }

    public static String dumps(Object o) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class RandomString {
        private SecureRandom random = new SecureRandom();
        public String random(int len) {
            return new BigInteger(len*8, random).toString(32);
        }
    }

    public static String prettifyJsonNode(JsonNode node) {
        return prettifyJson(node.toString());
    }
    public static String writeJson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    public static void l(Object ...o) {
        String s = "";
        for(Object oo : o) {
            if(oo instanceof int[]) {
                s+=Arrays.toString((int[])oo)+" ";
                continue;
            }
            if(oo instanceof double[]) {
                s+=Arrays.toString((double[])oo)+" ";
                continue;
            }
            if(oo instanceof boolean[]) {
                s+=Arrays.toString((boolean[])oo)+" ";
                continue;
            }
            if(oo instanceof char[]) {
                s+=Arrays.toString((char[])oo)+" ";
                continue;
            }
            if(oo instanceof long[]) {
                s+= Arrays.toString((long[])oo)+" ";
                continue;
            }
            if(oo instanceof String[]) {
                s+=Arrays.toString((String[])oo)+" ";
                continue;
            }
            if(oo instanceof Object[]) {
                s+=Arrays.deepToString((Object[])oo)+" ";
                continue;
            }
            s += (oo.toString())+" ";
        }
        System.out.println(s);
    }
    public static class LatencyTimer extends Thread {
        private static char noname = 'A';
        private final String name;
        private LatPrinter printer;
        public AtomicLong[] bins = new AtomicLong[4000];
        public AtomicLong maxNanos;
        public AtomicLong lastCount = new AtomicLong(0);

        double[] pTiles = new double[]{1, 50, 75, 90, 95, 99, 99.9};


        public LatencyTimer(String name) {
            this(new LatDefaultPrinter(), name);
        }


        public LatencyTimer(Class name) {
            this(new LatDefaultPrinter(), name.getName());
        }
        public LatencyTimer() {
            this(new LatDefaultPrinter(), ""+noname);
            noname += 1;
        }

        public LatencyTimer(LatPrinter p) {
            this(p, "noname");
        }

        public LatencyTimer(LatPrinter p, String name) {
            this.name = name;
            this.printer = p;
            reset();
            setDaemon(true);
            start();
        }

        public void setPrinter(LatPrinter printer) {
            this.printer = printer;
        }

        public void count(long latencyNanos) {
            int index = 0;
            maxNanos.set(Math.max(maxNanos.get(), latencyNanos));
            while(latencyNanos >= 1000) {
                latencyNanos /= 1000;
                index+=1000;
            }


            bins[(int) Math.min(index + latencyNanos, bins.length-1)].incrementAndGet();
        }

        public void count() {
            long now = System.nanoTime();
            count(Math.max(now-lastCount.get(), 0));
            lastCount.set(now);
        }

        public void reset() {
            for(int i=0; i<bins.length; i++) {
                bins[i] = new AtomicLong(0);
            }
            maxNanos = new AtomicLong(0);
        }

        AtomicBoolean die = new AtomicBoolean(false);
        public void die(){
            die.set(true);
        }
        @Override
        public void run() {
            while(true) {
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(die.get()) {
                    return;
                }
                printer.log(name, snap());
            }
        }



        public interface LatPrinter {
            void log(String name, LatRet ret);
        }


        public void doLog() {
            printer.log(name, snap());
        }

        private LatRet snap() {
            long[] mybins = new long[bins.length];
            long mytotal = 0;
            for(int i=0; i<bins.length; i++) {
                mybins[i] = bins[i].get();
                mytotal+=mybins[i];
            }

            long myMaxNanos = maxNanos.get();

            double[] nanos = new double[pTiles.length];
            int index = 0;
            long cumulative = 0;
            for(int i=0; i<pTiles.length; i++) {
                long max = (long)((mytotal*pTiles[i])/100.0);
                while(index < mybins.length && mybins[index] + cumulative <  max) {
                    cumulative+=mybins[index];
                    index++;
                }

                long mul = 1;
                int temp = index;
                while(temp >= 1000) {
                    temp -= 1000;
                    mul *= 1000;
                }
                nanos[i] = (temp+1)*mul;
            }
            reset();
            return new LatRet(mytotal, myMaxNanos, nanos, pTiles);
        }

        public static class LatDefaultPrinter implements LatPrinter {
            @Override
            public void log(String name, LatRet ret) {
                log.debug("{}, {}",name, ret);
            }
        }


        public static class LatRet implements Serializable {
            public double[] nanos;
            public double[] pTiles;
            public long total;
            public long maxNanos;
            public long snapTimeMillis;

            @Override
            public String toString() {
                if(total==0) {
                    return "No data points";
                }
                DecimalFormat df = new DecimalFormat("###.##");
                String s = String.format("max:%s", timeFormat(maxNanos, df));
                for(int i = nanos.length-1; i>=0; i--) {
                    s+=pTiles[i]+"%:"+timeFormat(nanos[i], df);
                }
                return s;
            }

            public static String timeFormat(double t, DecimalFormat df) {
                if(t<1000) {
                    return df.format(t)+"ns ";
                } else if (t<1000_000) {
                    return df.format(t/1000)+"us ";
                } else if (t<1000_000_000){
                    return df.format(t/1000_000)+"ms ";
                } else {
                    return df.format(t/1000_000_000)+"s ";
                }
            }
            //for objectmapper
            public LatRet(){}
            public LatRet(long total, long maxNanos, double[] nanos, double[] pTiles) {
                this.nanos = nanos;
                this.pTiles = pTiles;
                this.total = total;
                this.maxNanos = maxNanos;
                this.snapTimeMillis = System.currentTimeMillis();
            }
        }
    }
    public static void main(String[] args) throws InterruptedException {
        //log.debug("{}", httpPostJson("http://posttestserver.com/post.php", "f u"));
        //log.debug("{}", httpPostJson("http://httpbin.org/post", "f u"));
        Utils u = new Utils();
        u.go();
    }

    private void go() throws InterruptedException {
        Timer t = new Timer("ASfd");
        int c = 0;
        for (int i = 0; i < 10000000; i++) {
            t.cumulativeCount(c++);
            Thread.sleep(100);
        }

    }

    public static class Timer extends Thread {

        private String name;
        long beginTime = -1, lastSnapshotTime = -1;
        AtomicLong opsSoFar = new AtomicLong(0);
        AtomicLong opsSinceLastSnapshot = new AtomicLong(0);
        private Printer printer;
        public AtomicBoolean enabled = new AtomicBoolean(true);

        public void reset() {
            lastSnapshotTime = beginTime = System.nanoTime();
            opsSinceLastSnapshot.set(0);
            opsSoFar.set(0);
//            log.debug("======resetting timer====");
        }

        public Timer(String name) {
            this(new DefaultPrinter(), name);
        }


        public Timer(Class name) {
            this(new DefaultPrinter(), name.getName());
        }

        public Timer(Printer p) {
            this(p, "noname");
        }

        public Timer(Printer p, String name) {
            this.name = name;
            this.printer = p;
            setDaemon(true);
            reset();
            start();
        }

        public void setPrinter(Printer printer) {
            this.printer = printer;
        }
        AtomicBoolean die = new AtomicBoolean(false);
        public void die(){
            die.set(true);
        }

        @Override
        public void run() {
            reset();

            while(true) {
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(enabled.get()) doLog();
                if(interrupted()) break;
                if(die.get()) {
                    break;
                }
            }
        }


        public void doLog() {
            printer.log(name, snap());
        }

        public void count(long opsSinceLastCall) {
            opsSinceLastSnapshot.addAndGet(opsSinceLastCall);
        }

        public void count() {
            opsSinceLastSnapshot.incrementAndGet();
        }

        public void cumulativeCount(long cumulative) {
            opsSinceLastSnapshot.set(cumulative - opsSoFar.get());
        }

        public Ret snap() {
            if(beginTime <0) throw new RuntimeException("not initialized");
            long now = System.nanoTime();
            long ops = opsSinceLastSnapshot.getAndSet(0);
            long cumulativeOps = opsSoFar.addAndGet(ops);

            double qps = ops *1e9/(now- lastSnapshotTime);
            double totalqps = (cumulativeOps)*1e9/(now - beginTime);

            lastSnapshotTime = now;

            return new Ret((long)qps, (long)totalqps, ops, cumulativeOps);
        }

        public static class Ret implements Serializable {
            public long qps, totalQps, ops, totalOps;

            public Ret(long qps, long totalQps, long ops, long totalOps) {
                this.qps = qps;
                this.totalQps = totalQps;
                this.ops = ops;
                this.totalOps = totalOps;
            }

            // for objectmapper
            public Ret(){}

            @Override
            public String toString() {
//                return "Ret{" +
//                        "qps=" + format(qps) +
//                        ", totalQps=" + format(totalQps) +
//                        ", ops=" + format(ops) +
//                        ", totalOps=" + format(totalOps) +
//                        '}';
                return String.format("req_per_sec: %s   total_requests:%s", format(qps), format(totalOps));
            }

            String format(long x) {
                String s = ""+x;
                StringBuilder ss = new StringBuilder();
                for (int i = 0; i <s.length() ; i++) {
                    if(i%3==0 && i>0) {
                        ss.append(",");
                    }
                    ss.append(s.charAt(s.length()-i-1));

                }
//                return ss.reverse().toString();
                return ss.reverse().toString();
            }
        }

        public interface Printer {
            void log(String name, Ret ret);
        }

        public static class DefaultPrinter implements Printer {
            @Override
            public void log(String name, Ret ret) {
                log.debug("{} {}",name, ret);
            }
        }
    }

    public static class BadStatusCodeException extends RuntimeException {
        public BadStatusCodeException(String s) {
            super(s);
        }
    }
}
