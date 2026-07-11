import { spawnSync } from 'node:child_process';

const result = spawnSync('vue-tsc', ['--noEmit', '-p', 'tsconfig.ontology.json'], {
	encoding: 'utf8',
	env: process.env,
});
const output = `${result.stdout || ''}${result.stderr || ''}`;
const diagnosticLines = output.split(/\r?\n/).filter(Boolean);
const ontologyDiagnostics = diagnosticLines.filter((line) =>
	/^src\/(views\/ontology\/entity-type|views\/ontology\/data-property|views\/ontology\/object-property|views\/ontology\/axiom-rule|api\/ontology\/entity-type|api\/ontology\/data-property|api\/ontology\/object-property|api\/ontology\/axiom-rule|types\/ontology)\//.test(line)
);

if (ontologyDiagnostics.length > 0) {
	console.error(ontologyDiagnostics.join('\n'));
	process.exit(1);
}

const baselineDiagnostics = diagnosticLines.filter((line) => /^src\//.test(line)).length;
console.log(`本体建模模块类型检查通过；忽略项目既有的 ${baselineDiagnostics} 条非 ontology 诊断。`);
