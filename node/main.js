const process = require('process')

const warmUpBatchSize = 10_000
const msgSize = 1024

function mkMessage(n) {
  return new Uint8Array(msgSize).fill(n & 0xff);
}

function pushMsg(c, id) {
  const start = timeMicroseconds();
  c[id] = mkMessage(id);
  return (timeMicroseconds() - start);
}

function runBatch(msgCount, windowSize) {
  console.log(`Message count: ${msgCount}`);
  console.log(`Bugger size: ${windowSize}`);

  let stat = {
    msgCount: msgCount,
    max: -Infinity,
    // histograms of values for each of `i microsecond`
    // up to 500_000microseconds = 0.5s, should be enough for GC latency
    histogram: new Float64Array(500_000)
  }

  const start = timeMicroseconds();
  const c = new Array(windowSize);
  const allocated = timeMicroseconds() - start;
  for (let i = 0; i < msgCount; i++) {
    record(stat,
        pushMsg(c, i % windowSize)
    );
  }
  const elapsed = timeMicroseconds() - start;
  console.log(`Allocation time: ${allocated}µs`);
  console.log(`Push time stats:\n${statsToString(stat, msgCount)}`);
  console.log(`Total time: ${elapsed / 1_000_000}s`);
}

function warmUp(windowSize) {
  console.log("Warming-up...")
  runBatch(warmUpBatchSize, windowSize)
  console.log("...Warmed-up")
}

function main() {
  var args = process.argv.slice(2);
  const msgCount = parseInt(args[0], 10);
  const windowSize = parseInt(args[1], 10);
  warmUp(windowSize)
  runBatch(msgCount, windowSize);
}

main();

function timeMicroseconds() {
  const hrTime = process.hrtime()
  return hrTime[0] * 1_000_000 + hrTime[1] / 1000
}

function record(s, t) {
  if (t > s.max) {
    s.max = t
  }
  let index = t | 0;
  if (index >= s.histogram.length) {
    index = s.histogram.length -1
  }
  s.histogram[index]++
}

function statsToString(s) {
  return `max: ${s.max/1000}ms
  68.3 percentile: ${percentile(s, 0.683)}µs
  95.4 percentile: ${percentile(s, 0.954)}µs
  99.7 percentile: ${percentile(s, 0.997)}µs
  ${countLongerThanMs(s,10)} longer than 10ms
  ${countLongerThanMs(s,30)} longer than 30ms
  ${countLongerThanMs(s,100)} longer than 100ms`
}

function percentile(s, percent) {
  const valuesCount = Math.floor(s.msgCount * percent)
  let index = 0
  for (let n = 0; (index < s.histogram.length) && (n < valuesCount); index++) {
    n += s.histogram[index]
  }

  return index
}

function countLongerThanMs(s, ms) {
  let result = 0;
  for (let i = (ms * 1000); i < s.histogram.length; i++) {
    result += s.histogram[i];
  }

  return result;
}

