package main

import (
	"fmt"
	"os"
	"strconv"
	"time"
)

const (
	warmUpBatchSize = 10_000
	msgSize         = 1024
)

type message []byte
type channel []message

func mkMessage(n int) message {
	m := make(message, msgSize)
	for i := range m {
		m[i] = byte(n)
	}
	return m
}

func pushMsg(c *channel, id int) time.Duration {
	start := time.Now()
	m := mkMessage(id)
	(*c)[id] = m
	return time.Since(start)
}

func runBatch(msgCount int, windowSize int) {
	fmt.Println("Message count:", msgCount)
	fmt.Println("Bugger size: ", windowSize)

	var stats = Stats{msgCount: msgCount}

	start := time.Now()
	c := make(channel, windowSize)
	allocated := time.Since(start)
	for i := 0; i < msgCount; i++ {
		stats.record(
			pushMsg(&c, i%windowSize))
	}
	elapsed := time.Since(start)
	fmt.Println("Allocation time:", allocated.String())
	fmt.Println("Push time stats:\n", stats.String())
	fmt.Println("Total time:", elapsed.Seconds(), "s")
}

func warmUp(windowSize int) {
	fmt.Println("Warming-up...")
	runBatch(warmUpBatchSize, windowSize)
	fmt.Println("...Warmed-up")
}

func main() {
	var msgCount, _ = strconv.Atoi(os.Args[1])
	var windowSize, _ = strconv.Atoi(os.Args[2])
	warmUp(windowSize)
	runBatch(msgCount, windowSize)
}

type Stats struct {
	msgCount int
	max      time.Duration
	// histograms of values for each of `i microsecond`
	// up to 500_000microseconds = 0.5s, should be enough for GC latency
	histogram [500_000]int
}

func (s *Stats) record(t time.Duration) {
	if t > s.max {
		s.max = t
	}
	index := int(t.Microseconds())
	if index >= len(s.histogram) {
		index = len(s.histogram) - 1
	}
	s.histogram[index]++
}

func (s *Stats) String() string {
	return "max: " + s.max.String() +
		"\n68.3 percentile: " + s.percentile(0.683).String() +
		"\n95.4 percentile: " + s.percentile(0.954).String() +
		"\n99.7 percentile: " + s.percentile(0.997).String() +
		"\n" + s.countLongerThanMs(10) + " longer than 10ms" +
		"\n" + s.countLongerThanMs(30) + " longer than 30ms" +
		"\n" + s.countLongerThanMs(100) + " longer than 100ms"
}

func (s *Stats) percentile(percent float64) time.Duration {
	valuesCount := int(percent * float64(s.msgCount))
	index := 0
	for n := 0; (index < len(s.histogram)) && (n < valuesCount); index++ {
		n += s.histogram[index]
	}

	return time.Duration(index) * time.Microsecond
}

func (s *Stats) countLongerThanMs(ms int) string {
	result := 0
	for i := ms * 1000; i < len(s.histogram); i++ {
		result += s.histogram[i]
	}

	return strconv.Itoa(result)
}
