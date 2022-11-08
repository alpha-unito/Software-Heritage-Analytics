<div x-data={}>
    <x-table @updated-file.window="$wire.refresh()">
        <x-slot name="head">
            <x-table.heading sortable>{{ __('Name') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Tags') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Class') }}</x-table.heading>
            <x-table.heading class="whitespace-nowrap" sortable>{{ __('Creation Date') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Description') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Action') }}</x-table.heading>
        </x-slot>
        <x-slot name="body">
            @forelse ($applications as $application)
                <x-table.row class="border-b">
                    <x-table.cell>
                        <div class="flex flex-col">
                            <label class="font-bold whitespace-nowrap">
                                {{ $application->name }}
                            </label>
                        </div>
                    </x-table.cell>
                    <x-table.cell class="whitespace-nowrap">
                        @if ($application->tags && count($application->tags) > 0)
                            <div class="flex flex-row space-x-2">
                                @foreach ($application->tags as $tag)
                                    <span
                                        class="inline-flex items-center px-2.5 py-0.5 rounded-md text-sm font-medium bg-red-100 text-red-800">
                                        {{ $tag }}
                                    </span>
                                @endforeach
                            </div>
                        @endif
                    </x-table.cell>
                    <x-table.cell>
                        {{ $application->application_class }}
                    </x-table.cell>
                    <x-table.cell>
                        {{ $application->created_at->format("m/d/Y") }}
                    </x-table.cell>
                    <x-table.cell class="w-full">
                        {{ $application->description }}
                    </x-table.cell>
                    <x-table.cell>
                        <div class="flex flex-row space-x-2">
                            <a href="{{ route('application.download', ['application' => $application]) }}">
                                <x-button type="button" color="indigo">
                                    <div class="flex flex-row space-x-2 place-items-center">
                                        <x-icon-download class="w-5 font-red-700" />
                                        <label>Download</label>
                                    </div>
                                </x-button>
                            </a>
                            <x-button type="button"
                                x-on:click="$wire.delete({{ $application->id }}); $dispatch('delete-alert', {id: {{ $application->id }}})"
                                color="red">
                                <div class="flex flex-row space-x-2 place-items-center">
                                    <x-icon-delete class="w-3 font-red-700" />
                                    <label>Delete</label>
                                </div>
                            </x-button>
                        </div>
                    </x-table.cell>
                </x-table.row>
            @empty
                <x-table.row class="border-b">
                    <x-table.cell colspan="7">
                        <div class="flex justify-center font-semibold text-gray-700">
                            {{ __('Nothing to display') }}
                        </div>
                    </x-table.cell>
                </x-table.row>
            @endforelse
        </x-slot>
    </x-table>

    <x-modal @add-application-item.window="show= true" x-data="{ show: false, name: '', application_class: '', file: '', uploaded: false, uploadedFiles: [], saved: false }" @close-modal.window="show = false"
        x-show="show" wire:ignore>
        <x-slot name="icon">
        </x-slot>
        <x-slot name="title">
        </x-slot>
        <x-slot name="description">
            <div class="flex flex-col space-y-4">
                <div
                    class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                    <label for="name"
                        class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">Name</label>
                    <input x-bind:disabled="saved" wire:model="name" x-model="name" type="text" name="name" id="name"
                        class="block w-full p-0 text-gray-900 placeholder-gray-500 border-0 focus:ring-0 sm:text-sm"
                        placeholder="{{ __('Application Name') }}">
                </div>
                <div
                    class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                    <label for="tag"
                        class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">Tag</label>
                    <input x-bind:disabled="saved" wire:model="tags" type="text" name="tag" id="tag"
                        class="block w-full p-0 text-gray-900 placeholder-gray-500 border-0 focus:ring-0 sm:text-sm"
                        placeholder="{{ __('Comma Separated Tags') }}">
                </div>
                <div
                    class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                    <label for="description"
                        class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">Description</label>
                    <input x-bind:disabled="saved" wire:model="description" type="text" name="description"
                        id="description"
                        class="block w-full p-0 text-gray-900 placeholder-gray-500 border-0 focus:ring-0 sm:text-sm"
                        placeholder="{{ __('Application description') }}">
                </div>
                <div
                    class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                    <label for="application_class"
                        class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">Application Class</label>
                    <input x-bind:disabled="saved" wire:model="application_class" type="text" name="application_class"
                        id="application_class"
                        class="block w-full p-0 text-gray-900 placeholder-gray-500 border-0 focus:ring-0 sm:text-sm"
                        placeholder="{{ __('Application class') }}">
                </div>
                <div
                    class="relative px-3 py-2 border border-gray-300 rounded-md shadow-sm focus-within:ring-1 focus-within:ring-red-600 focus-within:border-red-600">
                    <label for="upload"
                        class="absolute inline-block px-1 -mt-px text-xs font-medium text-gray-900 bg-white -top-2 left-2">{{ __('Upload File') }}</label>
                    <div x-data="{ isUploading: false, progress: 0, error: false }" x-on:livewire-upload-start="isUploading = true"
                        x-on:livewire-upload-finish="isUploading = false; uploaded = true; "
                        x-on:livewire-upload-error="isUploading = false; error = true"
                        x-on:livewire-upload-progress="progress = $event.detail.progress">
                        <input id="upload_file" x-model="file" type="file" wire:model="file"
                            x-bind:disabled="name == '' || uploaded == true">

                        <label x-show="error">Errore caricamento file</label>
                        <div x-show="isUploading">
                            <progress class="w-full rounded-full" max="100" x-bind:value="progress"></progress>
                        </div>

                        <div class="flex flex-col">
                            <template x-for="uploadedFile in uploadedFiles">
                                <label>{{ __('Uploaded File') }}:<label class="ml-2 font-semibold text-red-500"
                                        x-text="uploadedFile"></label></label>
                            </template>
                        </div>
                    </div>
                </div>
            </div>
        </x-slot>
        <x-slot name="action">
            <div class="flex flex-row-reverse justify-between flex-grow">
                <button x-bind:disabled="name == '' || saved || file==''"
                    x-on:click="() => {$wire.save().then($wire.nextUpload().then(uploaded => { uploadedFiles.push(uploaded); $dispatch('updated-file'); saved = true}))}"
                    class="inline-flex items-center px-3 py-2 text-sm font-medium leading-4 text-red-700 bg-white border border-red-300 rounded-md shadow-sm hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-30">{{ __('Save') }}</button>
            </div>
            <div class="flex flex-row justify-between flex-grow">
                <x-button
                    x-on:click="$wire.clean().then( () => {show = false; name = ''; uploaded = false; uploadedFiles = []; let file = document.getElementById('upload_file'); file.value = ''; saved = false})"
                    color="indigo">{{ __('Close') }}</x-button>
            </div>
        </x-slot>
    </x-modal>
</div>
