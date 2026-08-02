# exam-flow Kubernetes 部署说明

## 环境

- 双可用区 K8s 集群(等保三级建议专有云/私有云)
- 镜像仓库:Harbor(示例 `harbor.example.gov.cn/examflow/`)
- 基础设施(MySQL MGR / Redis Cluster / RocketMQ 主从 / Nacos / MinIO)独立部署或托管,见 `../docker/docker-compose.yml` 为本地联调版

## 部署步骤

1. 命名空间与密钥:

   ```bash
   kubectl create namespace examflow
   kubectl -n examflow create secret generic examflow-secrets \
     --from-literal=exam-secret-key='<base64 32字节密钥>'
   ```

2. 部署服务(各服务独立 yaml,示例:考试服务):

   ```bash
   kubectl apply -f exam-service.yaml
   ```

3. 网关入口:网关 Service 以 LoadBalancer/Ingress 暴露,Nginx 反代接入(WAF 前置)。

## 考试期运维纪律(TDD §8.2)

- 开考前 2 小时至考试结束,禁止非紧急变更;
- 滚动更新 `maxUnavailable: 0`,保证无中断;
- 考试前运行巡检:`../scripts/pre-exam-check.sh`;
- 核心服务(考试/交卷)启用 HPA,上限按压测结果核定。

## 可观测性

- Prometheus + Grafana 采集(指标 /actuator/prometheus);
- SkyWalking 链路追踪(注入 agent);
- 告警接入 Alertmanager,分级规则见 TDD §8.1。
