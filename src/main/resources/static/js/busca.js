(function() {
    var tree = ARVORE_PASTAS;
    var selectedId = PASTA_SELECIONADA_ID;
    var container = document.getElementById('folderTree');
    if (!tree || !container) return;

    var STORAGE_KEY = 'sistemaPartiturasPastasAbertas';

    function getOpenedSet() {
        try {
            var data = JSON.parse(localStorage.getItem(STORAGE_KEY));
            return Array.isArray(data) ? new Set(data) : new Set();
        } catch (e) { return new Set(); }
    }

    function saveOpenedSet(set) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(set)));
    }

    function findAncestors(folders, targetId) {
        var result = [];
        function walk(list, path) {
            for (var i = 0; i < list.length; i++) {
                var f = list[i];
                var current = path.concat(f.id);
                if (f.id === targetId) {
                    for (var j = 0; j < current.length - 1; j++) {
                        result.push(current[j]);
                    }
                    return true;
                }
                if (f.subpastas && f.subpastas.length > 0) {
                    if (walk(f.subpastas, current)) return true;
                }
            }
            return false;
        }
        walk(folders, []);
        return result;
    }

    var openedSet = getOpenedSet();

    if (selectedId) {
        var ancestors = findAncestors(tree, selectedId);
        ancestors.forEach(function(id) { openedSet.add(id); });
        openedSet.add(selectedId);
        saveOpenedSet(openedSet);
    }

    function renderTree(folders, parentEl, level) {
        folders.forEach(function(folder) {
            var item = document.createElement('div');
            item.className = 'folder-item';
            if (folder.id === selectedId) item.classList.add('active');
            item.style.paddingLeft = (4 + level * 14) + 'px';

            var hasChildren = folder.subpastas && folder.subpastas.length > 0;
            var isOpen = openedSet.has(folder.id);

            var arrow = document.createElement('span');
            arrow.className = 'folder-arrow';
            if (hasChildren) {
                arrow.classList.add('has');
                arrow.textContent = isOpen ? '\u25BE' : '\u25B8';
            } else {
                arrow.classList.add('empty');
                arrow.textContent = '\u25B8';
            }

            var icon = document.createElement('span');
            icon.className = 'folder-icon';
            icon.textContent = '\uD83D\uDCC1';

            var label = document.createElement('span');
            label.className = 'folder-label';
            label.textContent = folder.nome;

            var link = document.createElement('a');
            link.className = 'folder-link';
            link.href = '/busca?pastaId=' + encodeURIComponent(folder.id);
            link.appendChild(icon);
            link.appendChild(label);

            item.appendChild(arrow);
            item.appendChild(link);
            parentEl.appendChild(item);

            if (hasChildren) {
                var childrenWrapper = document.createElement('div');
                childrenWrapper.className = 'folder-children';
                if (!isOpen) childrenWrapper.classList.add('collapsed');
                parentEl.appendChild(childrenWrapper);
                renderTree(folder.subpastas, childrenWrapper, level + 1);

                function toggleFolder() {
                    var currentlyOpen = openedSet.has(folder.id);
                    if (currentlyOpen) {
                        openedSet.delete(folder.id);
                        childrenWrapper.classList.add('collapsed');
                        arrow.textContent = '\u25B8';
                    } else {
                        openedSet.add(folder.id);
                        childrenWrapper.classList.remove('collapsed');
                        arrow.textContent = '\u25BE';
                    }
                    saveOpenedSet(openedSet);
                }

                arrow.addEventListener('click', function(e) {
                    e.stopPropagation();
                    toggleFolder();
                });

                label.addEventListener('click', function(e) {
                    e.stopPropagation();
                    toggleFolder();
                });
            }
        });
    }

    renderTree(tree, container, 0);

    // ── Sidebar collapse toggle ──
    var sidebarEl = document.querySelector('.sidebar');
    var collapseBtn = document.getElementById('sidebarCollapseBtn');
    var openHandle = document.getElementById('sidebarOpenHandle');
    var SIDEBAR_STORAGE_KEY = 'sistemaPartiturasSidebarCollapsed';

    function getSidebarState() {
        try {
            return localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
        } catch (e) { return false; }
    }

    function setSidebarState(collapsed) {
        try {
            localStorage.setItem(SIDEBAR_STORAGE_KEY, collapsed ? 'true' : 'false');
        } catch (e) {}
    }

    function toggleSidebar(collapsed) {
        var isCollapsed = (collapsed !== undefined) ? collapsed : sidebarEl.classList.contains('collapsed');
        if (collapsed !== undefined) {
            if (isCollapsed) {
                sidebarEl.classList.add('collapsed');
            } else {
                sidebarEl.classList.remove('collapsed');
            }
        } else {
            sidebarEl.classList.toggle('collapsed');
            isCollapsed = sidebarEl.classList.contains('collapsed');
        }
        setSidebarState(isCollapsed);
    }

    // Restore sidebar state on load
    if (getSidebarState()) {
        sidebarEl.classList.add('collapsed');
    }

    collapseBtn.addEventListener('click', function() {
        toggleSidebar(true);
    });

    openHandle.addEventListener('click', function() {
        toggleSidebar(false);
    });
})();
