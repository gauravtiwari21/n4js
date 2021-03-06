(function(System) {
	'use strict';
	System.register([], function($n4Export) {
		var Nvp;
		Nvp = function Nvp(spec) {
			this.name = spec && 'name' in spec ? spec.name : undefined;
			this.value = spec && 'value' in spec ? spec.value : undefined;
			if (spec) {}
		};
		$n4Export('Nvp', Nvp);
		return {
			setters: [],
			execute: function() {
				$makeClass(Nvp, Object, [], {
					name: {
						value: undefined,
						writable: true
					},
					value: {
						value: undefined,
						writable: true
					}
				}, {}, function(instanceProto, staticProto) {
					var metaClass = new N4Class({
						name: 'Nvp',
						origin: 'eu.numberfour.mangelhaft',
						fqn: 'n4.mangel.mangeltypes.Nvp.Nvp',
						n4superType: N4Object.n4type,
						allImplementedInterfaces: [],
						ownedMembers: [
							new N4DataField({
								name: 'name',
								isStatic: false,
								annotations: []
							}),
							new N4DataField({
								name: 'value',
								isStatic: false,
								annotations: []
							}),
							new N4Method({
								name: 'constructor',
								isStatic: false,
								jsFunction: instanceProto['constructor'],
								annotations: []
							})
						],
						consumedMembers: [],
						annotations: []
					});
					return metaClass;
				});
			}
		};
	});
})(typeof module !== 'undefined' && module.exports ? require('n4js-node/index').System(require, module) : System);
//# sourceMappingURL=Nvp.map
