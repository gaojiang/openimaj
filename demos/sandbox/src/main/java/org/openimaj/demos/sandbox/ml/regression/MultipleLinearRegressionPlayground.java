package org.openimaj.demos.sandbox.ml.regression;

import java.io.IOException;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openimaj.io.Cache;
import org.openimaj.ml.timeseries.IncompatibleTimeSeriesException;
import org.openimaj.ml.timeseries.aggregator.WindowedLinearRegressionAggregator;
import org.openimaj.ml.timeseries.collection.SynchronisedTimeSeriesCollection;
import org.openimaj.ml.timeseries.collection.TimeSeriesCollection;
import org.openimaj.ml.timeseries.processor.LinearRegressionProcessor;
import org.openimaj.ml.timeseries.processor.MovingAverageProcessor;
import org.openimaj.ml.timeseries.processor.WindowedLinearRegressionProcessor;
import org.openimaj.ml.timeseries.series.DoubleSynchronisedTimeSeriesCollection;
import org.openimaj.ml.timeseries.series.DoubleTimeSeries;
import org.openimaj.twitter.finance.YahooFinanceData;
import org.openimaj.util.pair.IndependentPair;
import org.terrier.utility.ArrayUtils;

/**
 * @author ss
 *
 */
public class MultipleLinearRegressionPlayground {
	/**
	 * @param args
	 * @throws IOException 
	 * @throws IncompatibleTimeSeriesException 
	 */
	public static void main(String[] args) throws IOException, IncompatibleTimeSeriesException {
		String start = "2010-01-01";
		String end = "2010-12-31";
		String learns = "2010-01-01";
		String learne = "2010-05-01";
		linearRegressStocks(start,end,learns,learne,"MSFT","AAPL");
		
		
	}

	private static void linearRegressStocks(String start, String end,String learns, String learne, String ...stocks) throws IncompatibleTimeSeriesException, IOException {
		DoubleSynchronisedTimeSeriesCollection dstsc = new DoubleSynchronisedTimeSeriesCollection();
		for (String stock : stocks ){			
			YahooFinanceData data = new YahooFinanceData(stock,start,end,"YYYY-MM-dd");
			data = Cache.load(data);
			DoubleTimeSeries highseries = data.seriesMap().get("High");
			DateTimeFormatter parser= DateTimeFormat.forPattern("YYYY-MM-dd");
			dstsc.addTimeSeries(stock, highseries);
		}
		TSCollection dataset = new TSCollection();
		timeSeriesToChart(dstsc,dataset);
		DoubleSynchronisedTimeSeriesCollection movingAverage = dstsc.processInternal(new MovingAverageProcessor(30l * 24l * 60l * 60l * 1000l));
		timeSeriesToChart(movingAverage,dataset,"-MA");		
		displayTimeSeries(dataset,ArrayUtils.join(stocks, " & "),"Date","Price");
		
		dataset = new TSCollection();
		DoubleTimeSeries interp = dstsc.series("AAPL").process(new WindowedLinearRegressionProcessor(10, 7));
		DoubleTimeSeries interpmsft = new WindowedLinearRegressionAggregator("AAPL", 10, 7, true).aggregate(dstsc);
		timeSeriesToChart("AAPL",dstsc.series("AAPL"),dataset);
		timeSeriesToChart("AAPL-interp",interp,dataset);
		timeSeriesToChart("AAPL-interpmstf",interpmsft,dataset);
		displayTimeSeries(dataset,ArrayUtils.join(stocks, " & ") + " Interp","Date","Price");
	}

	private static void displayTimeSeries(TSCollection dataset, String name, String xname, String yname) {
		JFreeChart chart = ChartFactory.createTimeSeriesChart(name,xname,yname, dataset, true, false, false);
		ChartPanel panel = new ChartPanel(chart);
		panel.setFillZoomRectangle(true);
		JFrame j = new JFrame();
		j.setContentPane(panel);
		j.pack();
		j.setVisible(true);
		j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private static void timeSeriesToChart(DoubleSynchronisedTimeSeriesCollection dstsc, TSCollection coll,String ... append) {
		for (String seriesName: dstsc.getNames()) {
			DoubleTimeSeries series = dstsc.series(seriesName);
			TimeSeries ret = new TimeSeries(seriesName + ArrayUtils.join(append, "-"));
			for (IndependentPair<Long, Double> pair : series) {
				DateTime dt = new DateTime(pair.firstObject());
				Day d = new Day(dt.getDayOfMonth(), dt.getMonthOfYear(), dt.getYear());
				ret.add(d,pair.secondObject());
			}
			coll.addSeries(ret);
		}
	}
	private static void timeSeriesToChart(String name, DoubleTimeSeries highseries, TSCollection coll) {
		TimeSeries ret = new TimeSeries(name);
		for (IndependentPair<Long, Double> pair : highseries) {
			DateTime dt = new DateTime(pair.firstObject());
			Day d = new Day(dt.getDayOfMonth(), dt.getMonthOfYear(), dt.getYear());
			ret.add(d,pair.secondObject());
		}
		coll.addSeries(ret);
	}
}
