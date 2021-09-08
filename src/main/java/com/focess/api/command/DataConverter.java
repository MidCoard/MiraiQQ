package com.focess.api.command;

public abstract class DataConverter<T> {

    public static final DataConverter<String> DEFAULT_DATA_CONVERTER = new DataConverter<String>() {
        @Override
        protected boolean accept(String arg) {
            return true;
        }

        @Override
        public String convert(String arg) {
            return arg;
        }

        @Override
        protected void connect(DataCollection dataCollection, String arg) {
            dataCollection.write(arg);
        }
    };


    public static final DataConverter<Integer> INTEGER_DATA_CONVERTER = new DataConverter<Integer>() {
        @Override
        protected boolean accept(String arg) {
            return TabCompleter.INTEGER_PREDICATE.test(arg);
        }

        @Override
        public Integer convert(String arg) {
            return Integer.parseInt(arg);
        }

        @Override
        protected void connect(DataCollection dataCollection, Integer arg) {
            dataCollection.writeInt(arg);
        }
    };
    public static final DataConverter<Long> LONG_DATA_CONVERTER = new DataConverter<Long>() {
        @Override
        protected boolean accept(String arg) {
            return TabCompleter.LONG_PREDICATE.test(arg);
        }

        @Override
        public Long convert(String arg) {
            return Long.parseLong(arg);
        }

        @Override
        protected void connect(DataCollection dataCollection, Long arg) {
            dataCollection.writeLong(arg);
        }
    };
    public static final DataConverter<Double> DOUBLE_DATA_CONVERTER = new DataConverter<Double>() {
        @Override
        protected boolean accept(String s) {
            return TabCompleter.DOUBLE_PREDICATE.test(s);
        }

        @Override
        public Double convert(String s) {
            return Double.parseDouble(s);
        }

        @Override
        protected void connect(DataCollection dataCollection, Double arg) {
            dataCollection.writeDouble(arg);
        }
    };

    protected abstract boolean accept(String arg);

    public abstract T convert(String arg);

    boolean put(DataCollection dataCollection, String arg) {
        if (this.accept(arg))
            this.connect(dataCollection, convert(arg));
        else return false;
        return true;
    }

    protected abstract void connect(DataCollection dataCollection, T arg);
}
