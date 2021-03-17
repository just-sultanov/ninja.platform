.EXPORT_ALL_VARIABLES:
.DEFAULT_GOAL := help
.PHONY: help test response schema

SHELL = bash

include .env



####
## Helpers
####

HEADER_CHAR=λ
HEADER_COLOR=\033[36m
TEXT_COLOR=\033[0m

define header
	@echo -e '$(HEADER_COLOR)${HEADER_CHAR}' $(1) '$(TEXT_COLOR)'
endef



####
## Global variables
####

PROJECT_ROOT=$(shell pwd)
CLJ_CONFIG=${PROJECT_ROOT}
BUILD = $(shell git rev-list --count master)


####
## ninja.platform
####

ARGS = $(filter-out $@,$(MAKECMDGOALS))
%:
	@:

help: ## Show help
	$(call header, "[${PROJECT_NAME}] Show help")
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "$(HEADER_COLOR)%-30s$(TEXT_COLOR) %s\n", $$1, $$2}' $(MAKEFILE_LIST)

deps: ## Check for deps updates
	$(call header, "[${PROJECT_NAME}] Check for deps updates")
	$(if $(strip $(ARGS)), cd $(ARGS) && clojure -M:project.updates/deps:project.updates/run, clojure -M:project.updates/deps:project.updates/run)

tree: ## Show deps tree
	$(call header, "[${PROJECT_NAME}] Show deps tree")
	$(if $(strip $(ARGS)), cd $(ARGS) && clojure -X:deps tree, clojure -X:deps tree)

clean: ## Clean
	$(call header, "[${PROJECT_NAME}] Clean")
	$(if $(strip $(ARGS)), @make clean-$(ARGS), @make clean-response clean-javac clean-fsm clean-schema)

repl: ## Run REPL
	$(call header, "[${PROJECT_NAME}] Run REPL")
	$(if $(strip $(ARGS)), @make repl-$(ARGS), $(error "Module name is not specified. Example, `$ make repl response`"))

lint: ## Run linter
	$(call header, "[${PROJECT_NAME}] Run linter")
	$(if $(strip $(ARGS)), @make lint-$(ARGS), @make lint-response lint-javac lint-fsm lint-schema)

format: ## Run formatter
	$(call header, "[${PROJECT_NAME}] Run formatter")
	$(if $(strip $(ARGS)), @make format-$(ARGS), @make format-response format-javac format-fsm format-schema)

test: ## Run tests
	$(call header, "[${PROJECT_NAME}] Run tests")
	$(if $(strip $(ARGS)), @make test-$(ARGS), @make test-response test-javac test-fsm test-schema)

coverage: ## Upload coverage
	$(call header, "[${PROJECT_NAME}] Upload coverage")
	$(if $(strip $(ARGS)), @make coverage-$(ARGS), @make coverage-response coverage-javac coverage-fsm coverage-schema)

pom: ## Generate pom
	$(call header, "[${PROJECT_NAME}] Generate pom")
	$(if $(strip $(ARGS)), @make pom-$(ARGS), @make pom-response pom-javac pom-fsm pom-schema)

jar: ## Build jar
	$(call header, "[${PROJECT_NAME}] Build jar")
	$(if $(strip $(ARGS)), @make jar-$(ARGS), @make jar-response jar-javac jar-fsm jar-schema)

install: ## Install jar
	$(call header, "[${PROJECT_NAME}] Install jar")
	$(if $(strip $(ARGS)), @make install-$(ARGS), @make install-response install-javac install-fsm install-schema)

release: ## Release
	$(call header, "[${PROJECT_NAME}] Release")
	$(if $(strip $(ARGS)), @make release-$(ARGS), $(error "Module name is not specified. Example, `$ make release response`"))

deploy: ## Deploy jar
	$(call header, "[${PROJECT_NAME}] Deploy jar")
	$(if $(strip $(ARGS)), @make deploy-$(ARGS), @make deploy-response deploy-javac deploy-fsm deploy-schema)



####
## ninja.platform/response
####

clean-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Clean")
	cd response && rm -rf target

repl-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Run REPL")
	cd response && clojure -M:project.bench/deps:project.bench/opts:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.clj/deps:project.test.cljs/deps:module.test/paths --main nrepl.cmdline --middleware '[cider.piggieback/wrap-cljs-repl]'

lint-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Run linter")
	cd response && clj-kondo --lint src/dev/clojure:src/main/clojure:src/test/clojure
	cljstyle check response/src

format-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Run formatter")
	cljstyle fix response/src

test-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Run tests")
	cd response && clojure -M:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.clj/deps:module.test/deps:module.test/paths --main kaocha.runner
	cd response && clojure -M:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.cljs/deps:module.test/deps:module.test/paths --main cljs-test-runner.main --dir src/test/clojure --out target/test-cljs

coverage-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Upload coverage")
	bash <(curl -s https://codecov.io/bash) -t ${CODECOV_TOKEN} -F response -f response/target/coverage/codecov.json

pom-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Generate pom")
	cd response && clojure -X:deps mvn-pom

jar-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Build jar")
	cd response && clojure -X:project.build/deps jar :group-id ${MODULE_NINJA_RESPONSE_GROUP_ID} :artifact-id ${MODULE_NINJA_RESPONSE_ARTIFACT_ID} :version '"${MODULE_NINJA_RESPONSE_VERSION}"' :sync-pom true :jar ${MODULE_NINJA_RESPONSE_JAR}

install-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Install jar")
	cd response && clojure -X:project.deploy/deps :installer :local :artifact '"${MODULE_NINJA_RESPONSE_JAR}"'

release-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Release")
	$(if $(strip $(shell git status --porcelain 2>/dev/null | grep response)),$(error You must commit all changes before bumping the project version. Bump failed),)
	git tag --annotate -m "Release ${MODULE_NINJA_RESPONSE_ARTIFACT_ID}/${MODULE_NINJA_RESPONSE_VERSION}" ${MODULE_NINJA_RESPONSE_ARTIFACT_ID}/${MODULE_NINJA_RESPONSE_VERSION}

deploy-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Deploy jar")
	cd response && clojure -X:project.deploy/deps :installer :remote :artifact '"${MODULE_NINJA_RESPONSE_JAR}"'



####
## ninja.platform/javac
####

clean-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Clean")
	cd javac && rm -rf target

repl-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Run REPL")
	cd javac && clojure -M:project.bench/deps:project.bench/opts:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.clj/deps:project.test.cljs/deps:module.test/paths --main nrepl.cmdline

lint-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Run linter")
	cd javac && clj-kondo --lint src/dev/clojure:src/main/clojure:src/test/clojure
	cljstyle check javac/src

format-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Run formatter")
	cljstyle fix javac/src

test-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Run tests")
	cd javac && clojure -M:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.clj/deps:module.test/deps:module.test/paths --main kaocha.runner

coverage-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Upload coverage")
	bash <(curl -s https://codecov.io/bash) -t ${CODECOV_TOKEN} -F javac -f javac/target/coverage/codecov.json

pom-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Generate pom")
	cd javac && clojure -X:deps mvn-pom

jar-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Build jar")
	cd javac && clojure -X:project.build/deps uberjar :group-id ${MODULE_NINJA_JAVAC_GROUP_ID} :artifact-id ${MODULE_NINJA_JAVAC_ARTIFACT_ID} :version '"${MODULE_NINJA_JAVAC_VERSION}"' :sync-pom true :jar ${MODULE_NINJA_JAVAC_JAR}

install-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Install jar")
	cd javac && clojure -X:project.deploy/deps :installer :local :artifact '"${MODULE_NINJA_JAVAC_JAR}"'

release-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Release")
	$(if $(strip $(shell git status --porcelain 2>/dev/null | grep javac)),$(error You must commit all changes before bumping the project version. Bump failed),)
	git tag --annotate -m "Release ${MODULE_NINJA_JAVAC_ARTIFACT_ID}/${MODULE_NINJA_JAVAC_VERSION}" ${MODULE_NINJA_JAVAC_ARTIFACT_ID}/${MODULE_NINJA_JAVAC_VERSION}

deploy-javac:
	$(call header, "[${MODULE_NINJA_JAVAC}] Deploy jar")
	cd javac && clojure -X:project.deploy/deps :installer :remote :artifact '"${MODULE_NINJA_JAVAC_JAR}"'



####
## ninja.platform/fsm
####

clean-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Clean")
	cd fsm && rm -rf target

repl-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Run REPL")
	cd fsm && clojure -M:project.bench/deps:project.bench/opts:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.clj/deps:project.test.cljs/deps:module.test/paths --main nrepl.cmdline --middleware '[cider.piggieback/wrap-cljs-repl]'

lint-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Run linter")
	cd fsm && clj-kondo --lint src/dev/clojure:src/main/clojure:src/test/clojure
	cljstyle check fsm/src

format-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Run formatter")
	cljstyle fix fsm/src

test-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Run tests")
	cd fsm && clojure -M:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.clj/deps:module.test/deps:module.test/paths --main kaocha.runner
	cd fsm && clojure -M:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.cljs/deps:module.test/deps:module.test/paths --main cljs-test-runner.main --dir src/test/clojure --out target/test-cljs

coverage-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Upload coverage")
	bash <(curl -s https://codecov.io/bash) -t ${CODECOV_TOKEN} -F fsm -f fsm/target/coverage/codecov.json

pom-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Generate pom")
	cd fsm && clojure -X:deps mvn-pom

jar-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Build jar")
	cd fsm && clojure -X:project.build/deps jar :group-id ${MODULE_NINJA_FSM_GROUP_ID} :artifact-id ${MODULE_NINJA_FSM_ARTIFACT_ID} :version '"${MODULE_NINJA_FSM_VERSION}"' :sync-pom true :jar ${MODULE_NINJA_FSM_JAR}

install-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Install jar")
	cd fsm && clojure -X:project.deploy/deps :installer :local :artifact '"${MODULE_NINJA_FSM_JAR}"'

release-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Release")
	$(if $(strip $(shell git status --porcelain 2>/dev/null | grep fsm)),$(error You must commit all changes before bumping the project version. Bump failed),)
	git tag --annotate -m "Release ${MODULE_NINJA_FSM_ARTIFACT_ID}/${MODULE_NINJA_FSM_VERSION}" ${MODULE_NINJA_FSM_ARTIFACT_ID}/${MODULE_NINJA_FSM_VERSION}

deploy-fsm:
	$(call header, "[${MODULE_NINJA_FSM}] Deploy jar")
	cd fsm && clojure -X:project.deploy/deps :installer :remote :artifact '"${MODULE_NINJA_FSM_JAR}"'



####
## ninja.platform/schema
####

clean-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Clean")
	cd schema && rm -rf target

repl-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Run REPL")
	cd schema && clojure -M:project.bench/deps:project.bench/opts:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.clj/deps:project.test.cljs/deps:module.test/paths --main nrepl.cmdline --middleware '[cider.piggieback/wrap-cljs-repl]'

lint-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Run linter")
	cd schema && clj-kondo --lint src/dev/clojure:src/main/clojure:src/test/clojure
	cljstyle check schema/src

format-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Run formatter")
	cljstyle fix schema/src

test-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Run tests")
	cd schema && clojure -M:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.clj/deps:module.test/deps:module.test/paths --main kaocha.runner
	cd schema && clojure -M:project.dev/deps:module.dev/deps:module.dev/paths:project.test/deps:project.test.cljs/deps:module.test/deps:module.test/paths --main cljs-test-runner.main --dir src/test/clojure --out target/test-cljs

coverage-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Upload coverage")
	bash <(curl -s https://codecov.io/bash) -t ${CODECOV_TOKEN} -F schema -f schema/target/coverage/codecov.json

pom-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Generate pom")
	cd schema && clojure -X:deps mvn-pom

jar-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Build jar")
	cd schema && clojure -X:project.build/deps jar :group-id ${MODULE_NINJA_SCHEMA_GROUP_ID} :artifact-id ${MODULE_NINJA_SCHEMA_ARTIFACT_ID} :version '"${MODULE_NINJA_SCHEMA_VERSION}"' :sync-pom true :jar ${MODULE_NINJA_SCHEMA_JAR}

install-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Install jar")
	cd schema && clojure -X:project.deploy/deps :installer :local :artifact '"${MODULE_NINJA_SCHEMA_JAR}"'

release-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Release")
	$(if $(strip $(shell git status --porcelain 2>/dev/null | grep schema)),$(error You must commit all changes before bumping the project version. Bump failed),)
	git tag --annotate -m "Release ${MODULE_NINJA_SCHEMA_ARTIFACT_ID}/${MODULE_NINJA_SCHEMA_VERSION}" ${MODULE_NINJA_SCHEMA_ARTIFACT_ID}/${MODULE_NINJA_SCHEMA_VERSION}

deploy-schema:
	$(call header, "[${MODULE_NINJA_SCHEMA}] Deploy jar")
	cd schema && clojure -X:project.deploy/deps :installer :remote :artifact '"${MODULE_NINJA_SCHEMA_JAR}"'
