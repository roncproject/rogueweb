# Rogue AWS – Complete Guide

> **Rogue: Exploring the Dungeons of Doom** converted from a local Swing
> desktop app into a Spring Boot web application that can be deployed to
> **Amazon AWS** and played in any browser.

---

## Table of Contents

1. [Architecture overview](#1-architecture-overview)
2. [How the app was converted](#2-how-the-app-was-converted)
3. [Local development on Windows](#3-local-development-on-windows)
4. [Running tests locally](#4-running-tests-locally)
5. [AWS deployment – Option A: App Runner (easiest)](#5-aws-deployment--option-a-app-runner-easiest)
6. [AWS deployment – Option B: ECS Fargate with Docker](#6-aws-deployment--option-b-ecs-fargate-with-docker)
7. [AWS deployment – Option C: Elastic Beanstalk (JAR)](#7-aws-deployment--option-c-elastic-beanstalk-jar)
8. [Environment variables](#8-environment-variables)
9. [Health checks and monitoring](#9-health-checks-and-monitoring)
10. [Security checklist](#10-security-checklist)
11. [Controls reference](#11-controls-reference)

---

## 1. Architecture overview

```
┌──────────────────────────────────────────────────────────────┐
│  Browser (any device)                                        │
│  ─────────────────                                           │
│  index.html  (Canvas renderer + REST client)                 │
│       │                                                      │
│       │  GET  /            – loads the game page             │
│       │  POST /api/new     – starts a fresh game             │
│       │  GET  /api/state   – fetches the current state       │
│       │  POST /api/key     – sends a keypress                │
│       │  GET  /actuator/health  – AWS health check           │
└───────┼──────────────────────────────────────────────────────┘
        │  HTTPS (AWS) or HTTP (local)
┌───────▼──────────────────────────────────────────────────────┐
│  Spring Boot server  (rogue-aws.jar)                         │
│  ──────────────────────────────────                          │
│  RogueApplication     – Spring Boot entry point              │
│  RogueController      – REST endpoints, key dispatch         │
│  GameSession          – per-session state (one game / tab)   │
│  ├── GameState        – dungeon map, player, monsters        │
│  ├── GameEngine       – movement, combat, item use           │
│  └── LevelGenerator   – procedural dungeon generation        │
└──────────────────────────────────────────────────────────────┘
        │
┌───────▼──────────────────────────────────────────────────────┐
│  AWS hosting layer (choose one)                              │
│  ─────────────────────────────                               │
│  App Runner  – zero-config, runs the Docker image            │
│  ECS Fargate – Docker container with load balancer           │
│  Elastic Beanstalk – upload the JAR, AWS manages the rest    │
└──────────────────────────────────────────────────────────────┘
```

Each browser session gets its own independent game — multiple players
can use the same server simultaneously.

---

## 2. How the app was converted

| Original (Swing desktop) | Converted (Spring Boot web) |
|--------------------------|-----------------------------|
| `RogueMain` starts a `JFrame` | `RogueApplication` starts embedded Tomcat |
| `GamePanel` receives `KeyEvent` from AWT | `RogueController.handleKey()` receives POST body |
| `GameRenderer` draws to `Graphics2D` | `RogueController.buildStateJson()` sends JSON; browser renders |
| Single global `GameState` | `@SessionScope GameSession` — one per browser tab |
| localhost only | Binds to `0.0.0.0`, reads `PORT` env-var |
| No health endpoint | `/actuator/health` added for AWS ALB checks |
| Manual classpath JAR | Maven fat JAR via `spring-boot-maven-plugin` |
| No tests | Spring MockMvc integration tests in `RogueControllerTest` |

The **game logic is 100% unchanged** (`GameState`, `GameEngine`,
`LevelGenerator`, `GameData`, `Room`, `Creature`, `Item`, `Stats`,
`Coord`). Only the presentation layer was replaced.

---

## 3. Local development on Windows

### Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17 or later | <https://adoptium.net> |
| Apache Maven | 3.9 or later | <https://maven.apache.org/download.cgi> |

#### Installing Java
1. Download the **JDK 17 LTS** `.msi` installer from <https://adoptium.net>.
2. Run the installer; tick **"Set JAVA_HOME variable"** and **"Add to PATH"**.
3. Open a new Command Prompt and verify: `java -version`

#### Installing Maven
1. Download the binary zip from <https://maven.apache.org/download.cgi>.
2. Extract to e.g. `C:\tools\apache-maven-3.9.x`.
3. Add `C:\tools\apache-maven-3.9.x\bin` to your **Path** environment variable.
4. Verify: `mvn -version`

### Build and run (one command)

```cmd
cd rogue-aws
run-local.bat
```

This script:
- Checks for Java and Maven
- Builds the fat JAR (`mvn package -DskipTests`)
- Starts the server on port 8080
- Opens `http://localhost:8080` in your default browser automatically

### Manual build + run

```cmd
cd rogue-aws

:: Build
mvn package -DskipTests

:: Run
java -jar target\rogue-aws.jar
```

Then open **`http://localhost:8080`** in your browser.

### Change the port

```cmd
:: Windows – set an environment variable before running
set PORT=9090
java -jar target\rogue-aws.jar
```

Or pass it as a Spring argument:
```cmd
java -jar target\rogue-aws.jar --server.port=9090
```

### Hot-reload during development

```cmd
mvn spring-boot:run
```

Spring Boot DevTools (not included by default) can be added for
live-reload, but it isn't required — restart the command above after
any Java change.

---

## 4. Running tests locally

```cmd
cd rogue-aws
mvn test
```

Test results are printed to the console and saved to
`target/surefire-reports/`.

The test class `RogueControllerTest` covers:
- `GET /actuator/health` → `{"status":"UP"}`
- `POST /api/new` → valid game state JSON
- `GET /api/state` → auto-starts a game
- Movement key → updated state
- `?` key → opens help popup
- Space → dismisses popup
- `GET /` → HTML frontend served

---

## 5. AWS deployment – Option A: App Runner (easiest)

AWS App Runner builds the Docker image for you from source stored in
a container registry. No EC2, no load balancer config needed.

### Step 1 – Prerequisites

- An AWS account with permissions to use ECR and App Runner.
- [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) installed and configured (`aws configure`).
- Docker Desktop installed and running.

### Step 2 – Build and push the Docker image to ECR

```cmd
:: Set your values
set REGION=eu-west-1
set ACCOUNT_ID=123456789012
set REPO_NAME=rogue-aws

:: Create the ECR repository (once only)
aws ecr create-repository --repository-name %REPO_NAME% --region %REGION%

:: Authenticate Docker to ECR
aws ecr get-login-password --region %REGION% | ^
  docker login --username AWS --password-stdin %ACCOUNT_ID%.dkr.ecr.%REGION%.amazonaws.com

:: Build the image
docker build -t %REPO_NAME% .

:: Tag and push
docker tag %REPO_NAME%:latest %ACCOUNT_ID%.dkr.ecr.%REGION%.amazonaws.com/%REPO_NAME%:latest
docker push %ACCOUNT_ID%.dkr.ecr.%REGION%.amazonaws.com/%REPO_NAME%:latest
```

### Step 3 – Create the App Runner service

In the AWS Console:

1. Open **AWS App Runner** → **Create service**.
2. **Source**: Container registry → Amazon ECR.
3. **Image URI**: `<account>.dkr.ecr.<region>.amazonaws.com/rogue-aws:latest`
4. **Deployment trigger**: Automatic (re-deploys when you push a new image).
5. **Port**: `8080`
6. **Health check path**: `/actuator/health`
7. **Instance**: 0.25 vCPU / 0.5 GB RAM is sufficient for casual play.
8. Click **Create & deploy**.

App Runner provides an HTTPS URL like
`https://abc123.eu-west-1.awsapprunner.com` — share that URL with
players.

### Step 4 – Update the deployment

```cmd
:: Rebuild and push
docker build -t %REPO_NAME% .
docker tag %REPO_NAME%:latest %ACCOUNT_ID%.dkr.ecr.%REGION%.amazonaws.com/%REPO_NAME%:latest
docker push %ACCOUNT_ID%.dkr.ecr.%REGION%.amazonaws.com/%REPO_NAME%:latest
:: App Runner auto-deploys the new image within ~2 minutes
```

---

## 6. AWS deployment – Option B: ECS Fargate with Docker

This option gives you more control (custom domain, multiple instances,
Auto Scaling) but requires more setup.

### Step 1 – Push the image to ECR

Same as Option A, Steps 1–2.

### Step 2 – Create an ECS Cluster

```cmd
aws ecs create-cluster --cluster-name rogue-cluster --region %REGION%
```

### Step 3 – Create a Task Definition

Save as `task-def.json`:

```json
{
  "family": "rogue-aws",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "rogue-aws",
      "image": "ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com/rogue-aws:latest",
      "portMappings": [{ "containerPort": 8080 }],
      "environment": [{ "name": "PORT", "value": "8080" }],
      "healthCheck": {
        "command": ["CMD-SHELL",
          "curl -sf http://localhost:8080/actuator/health | grep UP || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 20
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/rogue-aws",
          "awslogs-region": "eu-west-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

Register the task definition:
```cmd
aws ecs register-task-definition --cli-input-json file://task-def.json --region %REGION%
```

### Step 4 – Create a Load Balancer + Target Group

In the AWS Console:
1. **EC2 → Load Balancers → Create** → Application Load Balancer.
2. **Scheme**: Internet-facing; **Port**: 80 (HTTP) or 443 (HTTPS with ACM cert).
3. Create a **Target Group**: IP type, port 8080, health check path
   `/actuator/health`.
4. Note the ALB DNS name.

### Step 5 – Create the ECS Service

```cmd
aws ecs create-service ^
  --cluster rogue-cluster ^
  --service-name rogue-service ^
  --task-definition rogue-aws ^
  --desired-count 1 ^
  --launch-type FARGATE ^
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}" ^
  --load-balancers "targetGroupArn=arn:aws:...,containerName=rogue-aws,containerPort=8080" ^
  --region %REGION%
```

Players access the game at the ALB DNS name (e.g.
`http://rogue-alb-123456.eu-west-1.elb.amazonaws.com`).

---

## 7. AWS deployment – Option C: Elastic Beanstalk (JAR)

Elastic Beanstalk is the simplest option if you don't want to deal with
Docker — just upload the JAR file.

### Step 1 – Build the JAR

```cmd
mvn package -DskipTests
:: Creates: target/rogue-aws.jar
```

### Step 2 – Create a Procfile

Create a file called `Procfile` (no extension) in the project root:

```
web: java -jar rogue-aws.jar --server.port=$PORT
```

Zip it with the JAR:
```cmd
:: Windows
powershell Compress-Archive -Path target\rogue-aws.jar,Procfile -DestinationPath rogue-deploy.zip
```

### Step 3 – Deploy via AWS Console

1. Open **Elastic Beanstalk → Create Application**.
2. **Platform**: Java 17, Corretto.
3. **Application code**: Upload `rogue-deploy.zip`.
4. Wait for the environment to become **Health: OK**.
5. Click the provided URL to play.

### Step 4 – Update

```cmd
mvn package -DskipTests
powershell Compress-Archive -Path target\rogue-aws.jar,Procfile -DestinationPath rogue-deploy.zip
```

Then upload the new ZIP via the EB Console → **Upload and deploy**.

---

## 8. Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | HTTP port the server listens on. AWS sets this automatically. |
| `SERVER_SESSION_TIMEOUT` | `30m` | How long an idle game session is kept. |
| `LOGGING_LEVEL_ROOT` | `WARN` | Log verbosity (`DEBUG`, `INFO`, `WARN`, `ERROR`). |

Set in AWS App Runner via the service configuration → Environment variables.
Set in ECS via the task definition `environment` array.
Set in Elastic Beanstalk via Configuration → Software → Environment properties.

---

## 9. Health checks and monitoring

The app exposes Spring Boot Actuator at:

```
GET /actuator/health   →  {"status":"UP"}
GET /actuator/info     →  {"app":{"name":"Rogue...","version":"1.0"}}
```

These endpoints are used by:
- **App Runner** – health check path `/actuator/health`
- **ECS task health check** – curl command in the task definition
- **ALB target group** – health check path `/actuator/health`

No authentication is required for the health endpoint (it returns only
`{"status":"UP"}` or `{"status":"DOWN"}`, no sensitive data).

### CloudWatch logs

ECS and Elastic Beanstalk automatically send logs to CloudWatch.
App Runner also streams logs.

To view ECS logs from the CLI:
```cmd
aws logs tail /ecs/rogue-aws --follow --region %REGION%
```

---

## 10. Security checklist

| Item | Status |
|------|--------|
| App runs as non-root inside Docker | ✅ `USER rogue` in Dockerfile |
| No sensitive data in responses | ✅ Game state only |
| Health endpoint returns minimal info | ✅ `show-details=never` |
| HTTPS in production | ⚠️ Configure ACM cert on ALB or App Runner (free) |
| Sessions expire | ✅ 30-minute idle timeout |
| CORS locked down | Consider changing `@CrossOrigin(origins="*")` to your domain |
| Container resource limits | ✅ 256 CPU / 512 MB in task definition |

### Adding HTTPS (recommended)

**App Runner**: HTTPS is automatic — App Runner provisions and renews
the TLS certificate for its `awsapprunner.com` domain.

**ALB + ECS**: Request a free certificate in **AWS Certificate Manager**,
then add an HTTPS listener (port 443) to your ALB that forwards to the
target group on port 8080.

---

## 11. Controls reference

| Key | Action |
|-----|--------|
| Arrow keys / `h j k l` | Move one step |
| `y u b n` | Diagonal move |
| Shift + direction | Run in that direction |
| `,` | Pick up item |
| `d` | Drop item (choose from inventory) |
| `i` | Show inventory |
| `q` | Quaff (drink) a potion |
| `r` | Read a scroll |
| `e` | Eat food |
| `w` | Wield a weapon |
| `W` | Wear armor |
| `T` | Take off armor |
| `s` | Search for traps |
| `.` | Rest one turn |
| `>` | Descend stairs |
| `)` / `]` | Show equipped weapon / armor |
| `@` | Show character stats |
| `D` | Show identified items |
| `?` | Help |
| `Q` | Quit |
| `R` | Restart (on death / win screen) |
| Space / Esc | Close popup |
