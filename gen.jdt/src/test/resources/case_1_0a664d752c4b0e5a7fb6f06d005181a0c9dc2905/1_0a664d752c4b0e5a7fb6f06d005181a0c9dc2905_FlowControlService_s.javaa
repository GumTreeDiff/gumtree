 package org.red5.server.stream;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Timer;
 import java.util.TimerTask;
 
 import org.red5.server.api.IBandwidthConfigure;
 import org.red5.server.api.IFlowControllable;
 import org.red5.server.api.stream.support.SimpleBandwidthConfigure;
 import org.springframework.beans.BeansException;
 import org.springframework.context.ApplicationContext;
 import org.springframework.context.ApplicationContextAware;
 
 /**
  * An implementation of IFlowControlService.
  * TODO fairly distribute tokens across child nodes and elegantly
  * order the IFlowControllables for scheduling (give priority to buckets
  * that have threads waiting).
  * @author The Red5 Project (red5@osflash.org)
  * @author Steven Gong (steven.gong@gmail.com)
  */
 public class FlowControlService extends TimerTask
 implements IFlowControlService, ApplicationContextAware {
 	private long interval = 10;
 	private long defaultCapacity = 1024 * 100;
 	private Timer timer;
 	private Map<IFlowControllable, DataObject> fcsMap =
 		new HashMap<IFlowControllable, DataObject>();
 	private DummyBucket dummyBucket = new DummyBucket();
 	
 	public void registerFlowControllable(IFlowControllable fc) {
 		synchronized (fcsMap) {
 			if (fcsMap.containsKey(fcsMap)) return;
 			if (fc.getBandwidthConfigure() == null) return;
 			DataObject obj = new DataObject();
 			obj.bwConfig = new SimpleBandwidthConfigure(fc.getBandwidthConfigure());
 			long maxBurst = obj.bwConfig.getMaxBurst();
 			if (maxBurst <= 0) {
 				maxBurst = defaultCapacity;
 			}
 			long burst = obj.bwConfig.getBurst();
 			if (burst > maxBurst) {
 				burst = maxBurst;
 			} else if (burst < 0) {
 				burst = 0;
 			}
 			if (obj.bwConfig.getOverallBandwidth() >= 0) {
 				obj.overallBucket = new TokenBucket(burst);
 				obj.overallBucket.setCapacity(maxBurst);
 				obj.overallBucket.setSpeed(bps2Bpms(obj.bwConfig.getOverallBandwidth()));
 				obj.audioWrapper = new TokenBucketWrapper(obj.overallBucket);
 				obj.videoWrapper = new TokenBucketWrapper(obj.overallBucket);
 			} else {
 				obj.audioBucket = new TokenBucket(burst);
 				obj.audioBucket.setCapacity(maxBurst);
 				obj.audioBucket.setSpeed(bps2Bpms(obj.bwConfig.getAudioBandwidth()));
 				obj.videoBucket = new TokenBucket(burst);
 				obj.videoBucket.setCapacity(maxBurst);
 				obj.videoBucket.setSpeed(bps2Bpms(obj.bwConfig.getVideoBandwidth()));
 				obj.audioWrapper = new TokenBucketWrapper(obj.audioBucket);
 				obj.videoWrapper = new TokenBucketWrapper(obj.videoBucket);
 			}
 			fcsMap.put(fc, obj);
 		}
 	}
 
 	public void unregisterFlowControllable(IFlowControllable fc) {
 		synchronized (fcsMap) {
 			// TODO migrate the waiting threads to ancestors
 			fcsMap.remove(fc);
 		}
 	}
 
 	public void updateBWConfigure(IFlowControllable fc) {
 		synchronized (fcsMap) {
 			DataObject obj = fcsMap.get(fc);
 			if (obj == null) return;
 			if (fc.getBandwidthConfigure() == null) {
 				// simple unregister the flow controllable
 				// TODO migrate the waiting threads to ancestors
 				fcsMap.remove(fc);
 			}
 			IBandwidthConfigure oldConf = obj.bwConfig;
 			IBandwidthConfigure newConf = fc.getBandwidthConfigure();
 			long maxBurst = newConf.getMaxBurst();
 			if (maxBurst <= 0) {
 				maxBurst = defaultCapacity;
 			}
 			long burst = newConf.getBurst();
 			if (burst > maxBurst) {
 				burst = maxBurst;
 			} else {
 				burst = 0;
 			}
 			if (oldConf.getOverallBandwidth() >= 0 &&
 					newConf.getOverallBandwidth() >= 0) {
 				obj.overallBucket.setCapacity(newConf.getMaxBurst());
 				obj.overallBucket.setSpeed(bps2Bpms(newConf.getOverallBandwidth()));
 			} else if (oldConf.getOverallBandwidth() >= 0 &&
 					newConf.getOverallBandwidth() < 0) {
 				// TODO migrate waiting threads on overallBucket
 				// to a/v buckets
 				obj.overallBucket = null;
 				obj.audioBucket = new TokenBucket(burst);
 				obj.audioBucket.setCapacity(maxBurst);
 				obj.audioBucket.setSpeed(bps2Bpms(newConf.getAudioBandwidth()));
 				obj.videoBucket = new TokenBucket(burst);
 				obj.videoBucket.setCapacity(maxBurst);
 				obj.videoBucket.setSpeed(bps2Bpms(newConf.getVideoBandwidth()));
 				obj.audioWrapper.wrapped = obj.audioBucket;
 				obj.videoWrapper.wrapped = obj.videoBucket;
 			} else if (oldConf.getOverallBandwidth() < 0 &&
 					newConf.getOverallBandwidth() >= 0) {
 				// TODO migrate waiting threads on a/v buckets
 				// to overallBucket
 				obj.audioBucket = null;
 				obj.videoBucket = null;
 				obj.overallBucket = new TokenBucket(burst);
 				obj.overallBucket.setCapacity(maxBurst);
 				obj.overallBucket.setSpeed(bps2Bpms(newConf.getOverallBandwidth()));
 				obj.audioWrapper.wrapped = obj.overallBucket;
 				obj.videoWrapper.wrapped = obj.overallBucket;
 			} else {
 				obj.audioBucket.setCapacity(newConf.getMaxBurst());
 				obj.audioBucket.setSpeed(bps2Bpms(newConf.getAudioBandwidth()));
 				obj.videoBucket.setCapacity(newConf.getMaxBurst());
 				obj.videoBucket.setSpeed(bps2Bpms(newConf.getVideoBandwidth()));
 			}
 			obj.bwConfig = new SimpleBandwidthConfigure(newConf);
 		}
 	}
 
 	public void resetTokenBuckets(IFlowControllable fc) {
 		synchronized (fcsMap) {
 			DataObject obj = fcsMap.get(fc);
 			if (obj != null) {
 				if (obj.overallBucket != null) {
 					obj.overallBucket.reset();
 				}
 				if (obj.audioBucket != null) {
 					obj.audioBucket.reset();
 				}
 				if (obj.videoBucket != null) {
 					obj.videoBucket.reset();
 				}
 			}
 		}
 	}
 
 	public ITokenBucket getAudioTokenBucket(IFlowControllable fc) {
 		synchronized (fcsMap) {
 			DataObject obj = fcsMap.get(fc);
 			while (obj == null && fc.getParentFlowControllable() != null) {
 				fc = fc.getParentFlowControllable();
 				obj = fcsMap.get(fc);
 			}
 			if (obj != null) {
 				return obj.audioWrapper;
 			} else {
 				return dummyBucket;
 			}
 		}
 	}
 
 	public ITokenBucket getVideoTokenBucket(IFlowControllable fc) {
 		synchronized (fcsMap) {
 			DataObject obj = fcsMap.get(fc);
 			while (obj == null && fc.getParentFlowControllable() != null) {
 				fc = fc.getParentFlowControllable();
 				obj = fcsMap.get(fc);
 			}
 			if (obj != null) {
 				return obj.videoWrapper;
 			} else {
 				return dummyBucket;
 			}
 		}
 	}
 
 	public void setApplicationContext(ApplicationContext applicationContext)
 			throws BeansException {
 	}
 
 	@Override
 	public void run() {
 		synchronized (fcsMap) {
 			for (IFlowControllable fc : fcsMap.keySet()) {
 				// search through all parents to find the first ancestor that's
 				// registered IFlowControllable
 				ITokenBucket ancestorOverallBucket = null;
 				ITokenBucket ancestorAudioBucket = null;
 				ITokenBucket ancestorVideoBucket = null;
 				IFlowControllable parent = fc.getParentFlowControllable();
 				while (parent != null) {
 					if (fcsMap.containsKey(parent)) {
 						DataObject theObj = fcsMap.get(parent);
 						if (theObj.overallBucket != null) {
 							ancestorOverallBucket = theObj.overallBucket;
 						} else {
 							ancestorAudioBucket = theObj.audioBucket;
 							ancestorVideoBucket = theObj.videoBucket;
 						}
 						break;
 					}
 				}
 				if (ancestorOverallBucket == null &&
 						ancestorAudioBucket == null &&
 						ancestorVideoBucket == null) {
 					// no ancestors are registered, use the default one
 					ancestorVideoBucket = dummyBucket;
 					ancestorAudioBucket = dummyBucket;
 					ancestorOverallBucket = dummyBucket;
 				}
 				DataObject obj = fcsMap.get(fc);
 				if (obj.overallBucket != null) {
 					long tokenCount = obj.overallBucket.getSpeed() * interval;
 					long availableTokens = ancestorOverallBucket.acquireTokenBestEffort(tokenCount);
 					obj.overallBucket.addToken(availableTokens);
 				} else {
 					long tokenCount = obj.audioBucket.getSpeed() * interval;
 					long availableTokens = ancestorAudioBucket.acquireTokenBestEffort(tokenCount);
 					obj.audioBucket.addToken(availableTokens);
 					tokenCount = obj.videoBucket.getSpeed() * interval;
 					availableTokens = ancestorVideoBucket.acquireTokenBestEffort(tokenCount);
 					obj.videoBucket.addToken(availableTokens);
 				}
 			}
 		}
 	}
 	
 	public void init() {
 		timer = new Timer("FlowControlService", true);
 		timer.schedule(this, interval, interval);
 	}
 	
 	public void setInterval(long interval) {
 		this.interval = interval;
 	}
 	
 	public void setDefaultCapacity(long defaultCapacity) {
 		this.defaultCapacity = defaultCapacity;
 	}
 	
 	private long bps2Bpms(long bps) {
 		return bps / 1000 / 8;
 	}
 
 	private class DataObject {
 		private IBandwidthConfigure bwConfig;
 		private TokenBucketWrapper audioWrapper;
 		private TokenBucketWrapper videoWrapper;
 		private TokenBucket videoBucket;
 		private TokenBucket audioBucket;
 		private TokenBucket overallBucket;
 	}
 	
 	private class TokenBucketWrapper implements ITokenBucket {
 		private ITokenBucket wrapped;
 		
 		public TokenBucketWrapper(ITokenBucket wrapped) {
 			this.wrapped = wrapped;
 		}
 
 		public boolean acquireToken(long tokenCount, long wait) {
 			return wrapped.acquireToken(tokenCount, wait);
 		}
 
 		public long acquireTokenBestEffort(long upperLimitCount) {
 			return wrapped.acquireTokenBestEffort(upperLimitCount);
 		}
 
 		public boolean acquireTokenNonblocking(long tokenCount, ITokenBucketCallback callback) {
 			return wrapped.acquireTokenNonblocking(tokenCount, callback);
 		}
 
 		public long getCapacity() {
 			return wrapped.getCapacity();
 		}
 
 		public long getSpeed() {
 			return wrapped.getSpeed();
 		}
 
 		public void reset() {
 			wrapped.reset();
 		}
 	}
 	
 	/**
 	 * A bucket that always has token available.
 	 */
 	private class DummyBucket implements ITokenBucket {
 
 		public boolean acquireToken(long tokenCount, long wait) {
 			return true;
 		}
 
 		public long acquireTokenBestEffort(long upperLimitCount) {
 			return upperLimitCount;
 		}
 
 		public boolean acquireTokenNonblocking(long tokenCount, ITokenBucketCallback callback) {
 			return true;
 		}
 
 		public long getCapacity() {
 			return 0;
 		}
 
 		public long getSpeed() {
 			return 0;
 		}
 
 		public void reset() {
 		}
 	}
 }
