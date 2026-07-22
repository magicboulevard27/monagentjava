{{- define "monagentjava.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "monagentjava.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "monagentjava.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "monagentjava.labels" -}}
app.kubernetes.io/name: {{ include "monagentjava.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/component: monagent
{{- end -}}

{{- define "monagentjava.selectorLabels" -}}
app.kubernetes.io/name: {{ include "monagentjava.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "monagentjava.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "monagentjava.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
default
{{- end -}}
{{- end -}}
