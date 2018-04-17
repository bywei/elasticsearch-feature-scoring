/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jje.elasticsearch.plugins.scoring;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.script.AbstractDoubleSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

public class FeatureScoringScriptFactory implements NativeScriptFactory {

	@Override
	public ExecutableScript newScript(@Nullable Map<String, Object> params) {
		return new ScoringScript(params);
	}
	
	protected class ScoringScript extends AbstractDoubleSearchScript {
		private String customSortField = null;
		private Map<String, Integer> customSorts = new HashMap<String,Integer>();
		private Map<String, Integer> sortFields = new HashMap<String,Integer>();

		@SuppressWarnings("unchecked")
		public ScoringScript(@Nullable Map<String,Object> params){
			Object csField = params.get("customSortField");
			if(csField != null){
				this.customSortField = (String) csField;
				Object cs = params.get("customSorts");
				if(cs != null){
					this.customSorts = (Map<String, Integer>) cs;
				}
			}
			Object sf = params.get("sortFields");
			if(sf != null){
				this.sortFields = (Map<String, Integer>) sf;
			}
		}
		
		@Override
		public double runAsDouble() {
			double sortScore = 0;
			if(getCustomSortField() != null){
				long docId = ((ScriptDocValues.Longs)doc().get(getCustomSortField())).getValue();
				Integer customScore = getCustomScore(String.valueOf(docId));
				sortScore = (customScore != null) ? customScore.doubleValue() : 0;
			}
			for(String field : this.getSortFields().keySet()){
				long docFieldValue = ((ScriptDocValues.Longs)doc().get(field)).getValue();
				sortScore += docFieldValue * this.getSortFields().get(field).doubleValue();
			}
			return sortScore;
		}
		
		private Integer getCustomScore(String docId){
			return this.getCustomSorts().get(docId);
		}

		public Map<String, Integer> getCustomSorts() {
			return customSorts;
		}

		public Map<String, Integer> getSortFields() {
			return sortFields;
		}

		public String getCustomSortField() {
			return customSortField;
		}
	}

	@Override
	public boolean needsScores() {
		return false;
	}
}