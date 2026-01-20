# JMeter 성능 테스트 가이드

## 개요

주차장 검색 API 성능 테스트를 위한 JMeter 설정 가이드입니다.

---

## 1. JMeter 설치 및 실행

### 1.1 설치

```bash
# macOS
brew install jmeter

# Ubuntu/Debian
sudo apt-get install jmeter

# Windows (Chocolatey)
choco install jmeter

# 또는 직접 다운로드
# https://jmeter.apache.org/download_jmeter.cgi
```

### 1.2 실행

```bash
# GUI 모드 (테스트 계획 작성용)
jmeter

# CLI 모드 (실제 테스트용, 권장)
jmeter -n -t test_plan.jmx -l results.jtl -e -o report/
```

---

## 2. 테스트 계획 구조

### 2.1 기본 구조

```
Test Plan
├── Thread Group (사용자 시뮬레이션)
│   ├── HTTP Header Manager
│   ├── CSV Data Set Config (테스트 데이터)
│   ├── HTTP Request (API 호출)
│   └── Response Assertion
├── Listener (결과 수집)
│   ├── Summary Report
│   ├── Aggregate Report
│   └── Response Time Graph
└── Backend Listener (실시간 모니터링)
```

---

## 3. 테스트 시나리오

### 3.1 Baseline 테스트

| 항목 | 설정값 |
|------|--------|
| 동시 사용자 | 100 |
| Ramp-up 시간 | 60초 |
| 테스트 시간 | 5분 |
| 목적 | 기준 성능 측정 |

### 3.2 Stress 테스트

| 항목 | 설정값 |
|------|--------|
| 동시 사용자 | 500 |
| Ramp-up 시간 | 120초 |
| 테스트 시간 | 10분 |
| 목적 | 최대 처리량 확인 |

### 3.3 Spike 테스트

| 항목 | 설정값 |
|------|--------|
| 초기 사용자 | 50 |
| 최대 사용자 | 1000 |
| Spike 시간 | 10초 |
| 목적 | 급격한 부하 대응 확인 |

### 3.4 Endurance 테스트

| 항목 | 설정값 |
|------|--------|
| 동시 사용자 | 200 |
| 테스트 시간 | 30분 |
| 목적 | 메모리 누수, GC 문제 확인 |

---

## 4. JMeter 테스트 계획 (JMX)

### 4.1 parking_search_test.jmx

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Parking Search API Test">
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <stringProp name="TestPlan.comments">주차장 검색 API 성능 테스트</stringProp>
    </TestPlan>
    <hashTree>
      <!-- User Defined Variables -->
      <Arguments guiclass="ArgumentsPanel" testclass="Arguments" testname="Variables">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">localhost</stringProp>
          </elementProp>
          <elementProp name="PORT" elementType="Argument">
            <stringProp name="Argument.name">PORT</stringProp>
            <stringProp name="Argument.value">8080</stringProp>
          </elementProp>
        </collectionProp>
      </Arguments>
      <hashTree/>

      <!-- Thread Group -->
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Search Users">
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <boolProp name="LoopController.continue_forever">true</boolProp>
          <intProp name="LoopController.loops">-1</intProp>
        </elementProp>
      </ThreadGroup>
      <hashTree>
        <!-- CSV Data Set Config -->
        <CSVDataSet guiclass="TestBeanGUI" testclass="CSVDataSet" testname="Location Data">
          <stringProp name="filename">test_locations.csv</stringProp>
          <stringProp name="variableNames">lat,lng,radius</stringProp>
          <stringProp name="delimiter">,</stringProp>
          <boolProp name="recycle">true</boolProp>
          <boolProp name="stopThread">false</boolProp>
          <stringProp name="shareMode">shareMode.all</stringProp>
        </CSVDataSet>
        <hashTree/>

        <!-- HTTP Header Manager -->
        <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="Headers">
          <collectionProp name="HeaderManager.headers">
            <elementProp name="Content-Type" elementType="Header">
              <stringProp name="Header.name">Content-Type</stringProp>
              <stringProp name="Header.value">application/json</stringProp>
            </elementProp>
            <elementProp name="Accept" elementType="Header">
              <stringProp name="Header.name">Accept</stringProp>
              <stringProp name="Header.value">application/json</stringProp>
            </elementProp>
          </collectionProp>
        </HeaderManager>
        <hashTree/>

        <!-- HTTP Request - ES Search -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="ES Search">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.port">${PORT}</stringProp>
          <stringProp name="HTTPSampler.protocol">http</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/parkings/search</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <elementProp name="HTTPSampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="lat" elementType="HTTPArgument">
                <stringProp name="Argument.name">lat</stringProp>
                <stringProp name="Argument.value">${lat}</stringProp>
              </elementProp>
              <elementProp name="lng" elementType="HTTPArgument">
                <stringProp name="Argument.name">lng</stringProp>
                <stringProp name="Argument.value">${lng}</stringProp>
              </elementProp>
              <elementProp name="radius" elementType="HTTPArgument">
                <stringProp name="Argument.name">radius</stringProp>
                <stringProp name="Argument.value">${radius}</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
        <hashTree>
          <!-- Response Assertion -->
          <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Status Check">
            <collectionProp name="Asserion.test_strings">
              <stringProp name="49586">200</stringProp>
            </collectionProp>
            <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
            <intProp name="Assertion.test_type">8</intProp>
          </ResponseAssertion>
          <hashTree/>

          <!-- JSON Assertion -->
          <JSONPathAssertion guiclass="JSONPathAssertionGui" testclass="JSONPathAssertion" testname="JSON Check">
            <stringProp name="JSON_PATH">$</stringProp>
            <boolProp name="EXPECT_NULL">false</boolProp>
            <boolProp name="INVERT">false</boolProp>
            <boolProp name="ISREGEX">false</boolProp>
          </JSONPathAssertion>
          <hashTree/>
        </hashTree>

        <!-- HTTP Request - MySQL Search (비교용) -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="MySQL Search">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.port">${PORT}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/parkings/search/mysql</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <elementProp name="HTTPSampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="lat" elementType="HTTPArgument">
                <stringProp name="Argument.name">lat</stringProp>
                <stringProp name="Argument.value">${lat}</stringProp>
              </elementProp>
              <elementProp name="lng" elementType="HTTPArgument">
                <stringProp name="Argument.name">lng</stringProp>
                <stringProp name="Argument.value">${lng}</stringProp>
              </elementProp>
              <elementProp name="radius" elementType="HTTPArgument">
                <stringProp name="Argument.name">radius</stringProp>
                <stringProp name="Argument.value">${radius}</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
        <hashTree/>

        <!-- Constant Throughput Timer (TPS 제어) -->
        <ConstantThroughputTimer guiclass="TestBeanGUI" testclass="ConstantThroughputTimer" testname="Throughput Timer">
          <intProp name="calcMode">1</intProp>
          <doubleProp>
            <name>throughput</name>
            <value>600.0</value>
          </doubleProp>
        </ConstantThroughputTimer>
        <hashTree/>
      </hashTree>

      <!-- Summary Report -->
      <ResultCollector guiclass="SummaryReport" testclass="ResultCollector" testname="Summary Report">
        <boolProp name="ResultCollector.error_logging">false</boolProp>
        <objProp>
          <name>saveConfig</name>
          <value class="SampleSaveConfiguration">
            <time>true</time>
            <latency>true</latency>
            <timestamp>true</timestamp>
            <success>true</success>
            <label>true</label>
            <code>true</code>
            <message>true</message>
            <threadName>true</threadName>
            <bytes>true</bytes>
            <sentBytes>true</sentBytes>
            <url>true</url>
          </value>
        </objProp>
        <stringProp name="filename">results.jtl</stringProp>
      </ResultCollector>
      <hashTree/>

      <!-- Aggregate Report -->
      <ResultCollector guiclass="StatVisualizer" testclass="ResultCollector" testname="Aggregate Report">
        <boolProp name="ResultCollector.error_logging">false</boolProp>
        <stringProp name="filename">aggregate.jtl</stringProp>
      </ResultCollector>
      <hashTree/>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

---

## 5. 테스트 데이터

### 5.1 test_locations.csv

```csv
lat,lng,radius
37.5665,126.9780,3
37.4979,127.0276,5
37.5172,127.0473,3
37.5443,127.0557,5
37.5091,127.0556,3
37.5175,126.9003,5
37.4837,126.9016,3
37.5013,127.0396,5
37.5030,127.0249,3
37.5636,126.9895,5
37.5326,126.9901,3
37.5227,127.0230,5
37.5283,126.9640,3
37.5100,127.0330,5
37.4969,127.0263,3
37.5133,127.1000,5
37.5400,127.0690,3
37.5700,127.0000,5
37.4800,127.0500,3
37.5500,126.9500,5
```

### 5.2 데이터 설명

- 서울 주요 지역 20개 좌표
- 반경 3km, 5km 랜덤 분포
- CSV 순환 사용으로 다양한 검색 패턴 시뮬레이션

---

## 6. CLI 실행

### 6.1 기본 실행

```bash
# 테스트 실행 + HTML 리포트 생성
jmeter -n \
  -t parking_search_test.jmx \
  -l results.jtl \
  -e -o report/

# 옵션 설명
# -n : Non-GUI 모드
# -t : 테스트 계획 파일
# -l : 결과 로그 파일
# -e : 테스트 후 리포트 생성
# -o : 리포트 출력 디렉토리
```

### 6.2 파라미터 오버라이드

```bash
# 사용자 수, 시간 변경
jmeter -n \
  -t parking_search_test.jmx \
  -l results.jtl \
  -Jusers=200 \
  -Jrampup=120 \
  -Jduration=600 \
  -e -o report/
```

### 6.3 분산 테스트

```bash
# Master 서버에서 실행
jmeter -n \
  -t parking_search_test.jmx \
  -R slave1,slave2,slave3 \
  -l results.jtl \
  -e -o report/
```

---

## 7. 결과 분석

### 7.1 주요 지표

| 지표 | 설명 | 목표 |
|------|------|------|
| **Throughput (TPS)** | 초당 처리 요청 수 | 1,000+ |
| **Average** | 평균 응답 시간 | < 100ms |
| **90% Line (P90)** | 90%ile 응답 시간 | < 200ms |
| **95% Line (P95)** | 95%ile 응답 시간 | < 300ms |
| **99% Line (P99)** | 99%ile 응답 시간 | < 500ms |
| **Error %** | 에러 비율 | < 0.1% |

### 7.2 결과 예시

```
Label           # Samples  Avg   Min   Max   P90   P95   P99   Error%  TPS
ES Search       10000      45    12    234   78    95    156   0.00%   823.5
MySQL Search    10000      187   34    892   312   445   678   0.02%   312.4
```

### 7.3 분석 포인트

1. **ES vs MySQL 비교**: ES가 약 2-5배 빠른지 확인
2. **P99 확인**: 꼬리 지연(Tail Latency) 확인
3. **Error Rate**: 에러 발생 패턴 분석
4. **TPS 곡선**: 사용자 증가에 따른 처리량 변화

---

## 8. 단계별 테스트 가이드

### 8.1 MySQL Only 테스트

```bash
# MySQL 엔드포인트만 테스트
jmeter -n \
  -t mysql_only_test.jmx \
  -l mysql_results.jtl \
  -e -o mysql_report/
```

### 8.2 ES Only 테스트

```bash
# ES 엔드포인트만 테스트
jmeter -n \
  -t es_only_test.jmx \
  -l es_results.jtl \
  -e -o es_report/
```

### 8.3 Redis 캐싱 효과 테스트

```bash
# 캐시 워밍업 후 테스트
# 1. 낮은 부하로 캐시 채우기
jmeter -n -t warmup_test.jmx -l warmup.jtl -Jusers=10 -Jduration=60

# 2. 본 테스트 (캐시 히트 기대)
jmeter -n -t parking_search_test.jmx -l cached_results.jtl -e -o cached_report/
```

---

## 9. 결과 비교 스크립트

### 9.1 compare_results.sh

```bash
#!/bin/bash

# 결과 파일 비교
echo "=== Performance Comparison ==="
echo ""

echo "MySQL Results:"
awk -F',' 'NR>1 {sum+=$2; count++} END {print "  Avg: " sum/count "ms"}' mysql_results.jtl

echo ""
echo "ElasticSearch Results:"
awk -F',' 'NR>1 {sum+=$2; count++} END {print "  Avg: " sum/count "ms"}' es_results.jtl

echo ""
echo "Redis Cached Results:"
awk -F',' 'NR>1 {sum+=$2; count++} END {print "  Avg: " sum/count "ms"}' cached_results.jtl
```

### 9.2 실행

```bash
chmod +x compare_results.sh
./compare_results.sh
```

---

## 10. 체크리스트

- [ ] JMeter 설치 완료
- [ ] 테스트 계획(JMX) 파일 생성
- [ ] 테스트 데이터(CSV) 준비
- [ ] Baseline 테스트 실행 (MySQL)
- [ ] ES 테스트 실행 및 비교
- [ ] Redis 캐싱 테스트 실행 및 비교
- [ ] Stress 테스트로 한계점 확인
- [ ] HTML 리포트 분석
- [ ] 최적화 전후 결과 비교 문서화

---

## 11. 참고 자료

- [JMeter User Manual](https://jmeter.apache.org/usermanual/index.html)
- [JMeter Best Practices](https://jmeter.apache.org/usermanual/best-practices.html)
- [JMeter Distributed Testing](https://jmeter.apache.org/usermanual/jmeter_distributed_testing_step_by_step.html)
